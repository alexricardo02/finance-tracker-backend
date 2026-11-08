package com.example.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyReplayIT extends AbstractApiIntegrationTest {

    private static Map<String, String> key(String value) {
        return Map.of("Idempotency-Key", value);
    }

    private int incomeCount(ApiUser user) {
        return jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, user.id());
    }

    private int expenseCount(ApiUser user) {
        return jdbc.queryForObject("SELECT count(*) FROM expenses WHERE user_id = ?", Integer.class, user.id());
    }

    @Test
    void postIncome_replayedKey_returnsSameResponseWithoutDuplicating() {
        ApiUser user = newUser();
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = incomeBody(100, "USD", TX_DATE, "once");

        ResponseEntity<String> first = call(HttpMethod.POST, "/api/incomes", user, body, key(key));
        ResponseEntity<String> replay = call(HttpMethod.POST, "/api/incomes", user, body, key(key));

        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getBody()).isEqualTo(first.getBody());
        assertThat(incomeCount(user)).isEqualTo(1);
    }

    @Test
    void postExpense_replayedKey_returnsSameResponseWithoutDuplicating() {
        ApiUser user = newUser();
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = expenseBody(10, "USD", TX_DATE, "once");

        ResponseEntity<String> first = call(HttpMethod.POST, "/api/expenses", user, body, key(key));
        ResponseEntity<String> replay = call(HttpMethod.POST, "/api/expenses", user, body, key(key));

        assertThat(replay.getStatusCode().value()).isEqualTo(201);
        assertThat(replay.getBody()).isEqualTo(first.getBody());
        assertThat(expenseCount(user)).isEqualTo(1);
    }

    @Test
    void post_noKey_createsEachTime() {
        ApiUser user = newUser();

        createIncome(user, 100, "a");
        createIncome(user, 100, "b");

        assertThat(incomeCount(user)).isEqualTo(2);
    }

    @Test
    void post_differentKeys_createEachTime() {
        ApiUser user = newUser();
        Map<String, Object> body = incomeBody(100, "USD", TX_DATE, "x");

        call(HttpMethod.POST, "/api/incomes", user, body, key(UUID.randomUUID().toString()));
        call(HttpMethod.POST, "/api/incomes", user, body, key(UUID.randomUUID().toString()));

        assertThat(incomeCount(user)).isEqualTo(2);
    }

    @Test
    void delete_replayedKey_returnsNoContentAgain() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        String key = UUID.randomUUID().toString();

        assertThat(call(HttpMethod.DELETE, "/api/incomes/" + income, user, null, key(key)).getStatusCode().value()).isEqualTo(204);
        assertThat(call(HttpMethod.DELETE, "/api/incomes/" + income, user, null, key(key)).getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void failedRequest_releasesKeySoRetryCanSucceed() {
        ApiUser user = newUser();
        String key = UUID.randomUUID().toString();

        ResponseEntity<String> invalid = call(HttpMethod.POST, "/api/incomes", user,
                incomeBody(-1, "USD", TX_DATE, "bad"), key(key));
        ResponseEntity<String> retry = call(HttpMethod.POST, "/api/incomes", user,
                incomeBody(1, "USD", TX_DATE, "good"), key(key));

        assertThat(invalid.getStatusCode().value()).isEqualTo(400);
        assertThat(retry.getStatusCode().value()).isEqualTo(201);
        assertThat(incomeCount(user)).isEqualTo(1);
    }

    @Test
    void sameKey_differentUser_doesNotReceiveFirstUsersResponse() {
        ApiUser alice = newUser();
        ApiUser bob = newUser();
        String key = UUID.randomUUID().toString();
        call(HttpMethod.POST, "/api/incomes", alice, incomeBody(100, "USD", TX_DATE, "alice-secret"), key(key));

        ResponseEntity<String> bobResponse = call(HttpMethod.POST, "/api/incomes", bob,
                incomeBody(5, "USD", TX_DATE, "bob-data"), key(key));

        assertThat(bobResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(bobResponse.getBody()).doesNotContain("alice-secret").contains("bob-data");
        assertThat(incomeCount(bob)).isEqualTo(1);
    }

    @Test
    void sameKey_noToken_isRejectedWithoutReplayingResponse() {
        ApiUser alice = newUser();
        String key = UUID.randomUUID().toString();
        call(HttpMethod.POST, "/api/incomes", alice, incomeBody(100, "USD", TX_DATE, "alice-secret"), key(key));

        ResponseEntity<String> anonymous = call(HttpMethod.POST, "/api/incomes", null,
                incomeBody(5, "USD", TX_DATE, "x"), key(key));

        assertThat(anonymous.getStatusCode().value()).isEqualTo(401);
        assertThat(anonymous.getBody() == null ? "" : anonymous.getBody()).doesNotContain("alice-secret");
    }

    @Test
    void storedKey_belongsToAuthenticatedUser() {
        ApiUser user = newUser();
        String key = UUID.randomUUID().toString();

        call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, "USD", TX_DATE, "x"), key(key));

        assertThat(jdbc.queryForObject("SELECT user_id FROM idempotency_keys WHERE idempotency_key = ?", Integer.class, key))
                .isEqualTo(user.id());
    }

    @Test
    void anonymousRequest_doesNotBlockKeyForLaterAuthenticatedUse() {
        ApiUser user = newUser();
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = incomeBody(100, "USD", TX_DATE, "x");

        call(HttpMethod.POST, "/api/incomes", null, body, key(key));

        assertThat(call(HttpMethod.POST, "/api/incomes", user, body, key(key)).getStatusCode().value()).isEqualTo(201);
        assertThat(incomeCount(user)).isEqualTo(1);
    }
}
