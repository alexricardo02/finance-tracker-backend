package com.example.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitIT extends AbstractApiIntegrationTest {

    private static final Map<String, String> FIXED_CLIENT = Map.of("X-Forwarded-For", "198.51.100.7");
    private static final Map<String, Object> BAD_LOGIN = Map.of("username", "nobody", "password", "WrongPass1");

    private int requestsUntilThrottled(int maxAttempts, Supplier<ResponseEntity<String>> request) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (request.get().getStatusCode().value() == 429) {
                return attempt;
            }
        }
        return -1;
    }

    @Test
    void login_overLimit_returns429WithRetryAfter() {
        for (int i = 0; i < 5; i++) {
            assertThat(call(HttpMethod.POST, "/api/users/login", null, BAD_LOGIN, FIXED_CLIENT).getStatusCode().value()).isEqualTo(401);
        }

        ResponseEntity<String> throttled = call(HttpMethod.POST, "/api/users/login", null, BAD_LOGIN, FIXED_CLIENT);

        assertThat(throttled.getStatusCode().value()).isEqualTo(429);
        assertThat(Long.parseLong(throttled.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))).isPositive();
        assertThat(json(throttled).get("retryAfterSeconds").asLong()).isPositive();
    }

    @Test
    void register_overLimit_returns429() {
        assertThat(requestsUntilThrottled(5, () ->
                call(HttpMethod.POST, "/api/users/register", null, "{}", FIXED_CLIENT))).isEqualTo(4);
    }

    @Test
    void forgotPassword_overLimit_returns429() {
        assertThat(requestsUntilThrottled(5, () -> call(HttpMethod.POST, "/api/users/forgot-password", null,
                Map.of("email", "nobody@test.com"), FIXED_CLIENT))).isEqualTo(4);
    }

    @Test
    void refresh_overLimit_returns429() {
        assertThat(requestsUntilThrottled(30, () ->
                call(HttpMethod.POST, "/api/users/refresh", null, null, FIXED_CLIENT))).isBetween(21, 30);
    }

    @Test
    void login_spoofedLeftmostForwardedForEntry_doesNotEscapeLimit() {
        int[] counter = {0};
        int attempts = requestsUntilThrottled(10, () -> call(HttpMethod.POST, "/api/users/login", null, BAD_LOGIN,
                Map.of("X-Forwarded-For", "10.9.9." + (++counter[0]) + ", 198.51.100.7")));

        assertThat(attempts).isEqualTo(6);
    }

    @Test
    void login_differentClientsBehindProxy_haveSeparateBuckets() {
        Map<String, String> firstClient = Map.of("X-Forwarded-For", "198.51.100.7");
        Map<String, String> secondClient = Map.of("X-Forwarded-For", "198.51.100.8");

        assertThat(requestsUntilThrottled(10, () -> call(HttpMethod.POST, "/api/users/login", null, BAD_LOGIN, firstClient))).isEqualTo(6);
        assertThat(call(HttpMethod.POST, "/api/users/login", null, BAD_LOGIN, secondClient).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void categoryMutations_overPerUserLimit_return429AndOtherUserIsUnaffected() {
        ApiUser noisy = newUser();
        int[] counter = {0};

        int attempts = requestsUntilThrottled(90, () -> call(HttpMethod.POST, "/api/categories", noisy,
                Map.of("name", "c" + (++counter[0]), "type", "expense")));

        assertThat(attempts).isBetween(61, 90);
        assertThat(call(HttpMethod.POST, "/api/categories", newUser(),
                Map.of("name", "fine", "type", "expense")).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void incomeMutations_overPerUserLimit_return429() {
        ApiUser noisy = newUser();

        int attempts = requestsUntilThrottled(90, () ->
                call(HttpMethod.POST, "/api/incomes", noisy, incomeBody(1, "USD", TX_DATE, "x")));

        assertThat(attempts).isBetween(61, 90);
    }

    @Test
    void reads_areNotCountedAgainstMutationLimit() {
        ApiUser user = newUser();

        assertThat(requestsUntilThrottled(70, () -> call(HttpMethod.GET, "/api/categories", user, null))).isEqualTo(-1);
    }
}
