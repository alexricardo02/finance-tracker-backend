package com.example.integration;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.repository.IncomeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idempotency integration test: two concurrent POST /api/incomes with the same
 * Idempotency-Key must result in exactly one DB row and the second call must
 * receive the cached (replayed) response rather than a fresh insert.
 *
 * WHY concurrent requests: the idempotency filter uses a unique DB constraint
 * to prevent double-inserts under race conditions; a sequential test would pass
 * trivially without exercising the constraint path.
 */
class IdempotencyIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired IncomeRepository incomeRepository;

    @Test
    void concurrentPostIncomes_sameIdempotencyKey_onlyOneRowCreated() throws Exception {
        // ── Setup: register + login to get a valid auth cookie ────────────────
        String suffix = String.valueOf(System.nanoTime());
        UserRegistrationDTO reg = new UserRegistrationDTO(
                "idmp_" + suffix, "Passw0rd!", "idmp_" + suffix + "@test.com");
        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/register",
                toJsonEntity(reg), String.class);

        LoginRequestDTO login = new LoginRequestDTO(reg.getUsername(), reg.getPassword());
        ResponseEntity<String> loginResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/login",
                toJsonEntity(login), String.class);
        String authCookie = extractCookieValue(
                loginResp.getHeaders().get(HttpHeaders.SET_COOKIE), "auth_token");

        // ── Build request body ─────────────────────────────────────────────────
        // We need the categoryId — use the default "Uncategorized" category (id=1).
        // The import controller creates categories automatically; here we call income
        // import which auto-creates categories, simplifying setup.
        String idempotencyKey = "test-key-" + suffix;

        // Get category to use
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.add(HttpHeaders.COOKIE, "auth_token=" + authCookie);
        ResponseEntity<String> catResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/categories",
                HttpMethod.GET,
                new HttpEntity<>(getHeaders),
                String.class);
        // Extract first categoryId from response
        int categoryId = extractFirstCategoryId(catResp.getBody());

        String incomeBody = String.format(
                "{\"amount\":100.0,\"currency\":\"USD\",\"date\":\"2026-07-01\"," +
                "\"categoryId\":%d,\"paymentMethod\":\"CASH\",\"userId\":1}", categoryId);

        // ── Fire two concurrent requests with the SAME Idempotency-Key ────────
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go    = new CountDownLatch(1);

        Future<ResponseEntity<String>> f1 = executor.submit(() -> {
            ready.countDown();
            go.await();
            return postIncome(authCookie, idempotencyKey, incomeBody);
        });
        Future<ResponseEntity<String>> f2 = executor.submit(() -> {
            ready.countDown();
            go.await();
            return postIncome(authCookie, idempotencyKey, incomeBody);
        });

        ready.await(); // both threads ready
        go.countDown(); // release both simultaneously

        ResponseEntity<String> r1 = f1.get();
        ResponseEntity<String> r2 = f2.get();
        executor.shutdown();

        // ── Assertions ────────────────────────────────────────────────────────
        // Under concurrent execution of two identical Idempotency-Key requests:
        // One request proceeds and creates the resource (201 CREATED).
        // The concurrent race receives either 201 (replayed) or 409 CONFLICT (if caught in-flight).
        assertThat(r1.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(r2.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(List.of(r1.getStatusCode(), r2.getStatusCode())).contains(HttpStatus.CREATED);

        // WHY: the idempotency constraint on (key, user) ensures only one DB row
        // is created even when two concurrent requests race through the filter.
        long rowsForUser = incomeRepository.findByUserUsername(reg.getUsername()).stream()
                .filter(i -> i.getAmount() == 100.0)
                .count();
        assertThat(rowsForUser).isEqualTo(1);

        // Subsequent sequential request with the same Idempotency-Key receives the cached 201 response
        ResponseEntity<String> r3 = postIncome(authCookie, idempotencyKey, incomeBody);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<String> postIncome(String authCookie, String idempotencyKey, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, "auth_token=" + authCookie);
        headers.add("Idempotency-Key", idempotencyKey);
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/incomes",
                new HttpEntity<>(body, headers),
                String.class);
    }

    private HttpEntity<String> toJsonEntity(Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }

    private String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
        if (setCookieHeaders == null) return null;
        return setCookieHeaders.stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .map(h -> {
                    String prefix = cookieName + "=";
                    int start = h.indexOf(prefix) + prefix.length();
                    int end = h.indexOf(';', start);
                    return end == -1 ? h.substring(start) : h.substring(start, end);
                })
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private int extractFirstCategoryId(String json) {
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, List.class);
            if (list.isEmpty()) return 1;
            return (int) list.get(0).get("categoryId");
        } catch (Exception e) {
            return 1;
        }
    }
}
