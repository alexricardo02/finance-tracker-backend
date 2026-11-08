package com.example.integration;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EndpointSecurityIT extends AbstractApiIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                arguments(HttpMethod.GET, "/api/incomes"),
                arguments(HttpMethod.POST, "/api/incomes"),
                arguments(HttpMethod.GET, "/api/incomes/1"),
                arguments(HttpMethod.PUT, "/api/incomes/1"),
                arguments(HttpMethod.DELETE, "/api/incomes/1"),
                arguments(HttpMethod.GET, "/api/incomes/by-type?incomeType=Salary"),
                arguments(HttpMethod.GET, "/api/incomes/total-month?month=JULY"),
                arguments(HttpMethod.GET, "/api/expenses"),
                arguments(HttpMethod.POST, "/api/expenses"),
                arguments(HttpMethod.PUT, "/api/expenses/1"),
                arguments(HttpMethod.DELETE, "/api/expenses/1"),
                arguments(HttpMethod.GET, "/api/categories"),
                arguments(HttpMethod.POST, "/api/categories"),
                arguments(HttpMethod.DELETE, "/api/categories/1"),
                arguments(HttpMethod.GET, "/api/exports/transactions"),
                arguments(HttpMethod.POST, "/api/imports/transactions"),
                arguments(HttpMethod.GET, "/api/settings"),
                arguments(HttpMethod.PUT, "/api/settings/currency"),
                arguments(HttpMethod.GET, "/api/exchange-rate/usd-ars"),
                arguments(HttpMethod.DELETE, "/api/users/me"),
                arguments(HttpMethod.GET, "/actuator/metrics"),
                arguments(HttpMethod.GET, "/actuator/info"));
    }

    private ResponseEntity<String> anonymous(HttpMethod method, String path) {
        boolean hasBody = method == HttpMethod.POST || method == HttpMethod.PUT || path.equals("/api/users/me");
        return call(method, path, null, hasBody ? "{}" : null);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    void protectedEndpoint_noToken_isRejected(HttpMethod method, String path) {
        assertThat(anonymous(method, path).getStatusCode().value()).isEqualTo(401);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    void protectedEndpoint_noToken_returns401(HttpMethod method, String path) {
        assertThat(anonymous(method, path).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void protectedEndpoints_userRoleWithOwnData_return2xx() {
        ApiUser user = newUser();
        stubDolarOficial(1000.0);
        int income = createIncome(user, 100, "salary");
        int expense = createExpense(user, 10, "lunch");
        String importRow = "[{\"kind\":\"income\",\"amount\":5.0,\"currency\":\"USD\",\"date\":\"" + TX_DATE
                + "\",\"categoryName\":\"Salary\"}]";

        SoftAssertions softly = new SoftAssertions();
        expectStatus(softly, HttpMethod.GET, "/api/incomes", user, null, 200);
        expectStatus(softly, HttpMethod.GET, "/api/incomes/total-month?month=JULY", user, null, 200);
        expectStatus(softly, HttpMethod.PUT, "/api/incomes/" + income, user, incomeBody(101, "USD", TX_DATE, "x"), 200);
        expectStatus(softly, HttpMethod.GET, "/api/expenses", user, null, 200);
        expectStatus(softly, HttpMethod.PUT, "/api/expenses/" + expense, user, expenseBody(11, "USD", TX_DATE, "x"), 200);
        expectStatus(softly, HttpMethod.GET, "/api/categories", user, null, 200);
        expectStatus(softly, HttpMethod.GET, "/api/exports/transactions?format=csv", user, null, 200);
        expectStatus(softly, HttpMethod.GET, "/api/exports/transactions?format=xlsx", user, null, 200);
        expectStatus(softly, HttpMethod.GET, "/api/exports/transactions?format=pdf", user, null, 200);
        expectStatus(softly, HttpMethod.POST, "/api/imports/transactions", user, importRow, 201);
        expectStatus(softly, HttpMethod.GET, "/api/settings", user, null, 200);
        expectStatus(softly, HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "USD"), 200);
        expectStatus(softly, HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null, 200);
        expectStatus(softly, HttpMethod.GET, "/actuator/metrics", user, null, 200);
        expectStatus(softly, HttpMethod.DELETE, "/api/incomes/" + income, user, null, 204);
        expectStatus(softly, HttpMethod.DELETE, "/api/expenses/" + expense, user, null, 204);
        expectStatus(softly, HttpMethod.DELETE, "/api/users/me", user, Map.of("password", PASSWORD), 204);
        softly.assertAll();
    }

    @Test
    void getIncomeById_owner_returns200() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "salary");

        ResponseEntity<String> response = call(HttpMethod.GET, "/api/incomes/" + income, user, null);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        assertThat(json(response).get("incomeId").asInt()).isEqualTo(income);
    }

    @Test
    void deleteCategory_owner_returns204() {
        ApiUser user = newUser();
        int category = createCategory(user, "Mine", "expense");

        ResponseEntity<String> response = call(HttpMethod.DELETE, "/api/categories/" + category, user, null);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(204);
    }

    @Test
    void getIncomesByType_withMatchingRows_returns200() {
        ApiUser user = newUser();
        createIncome(user, 100, "salary");

        ResponseEntity<String> response = call(HttpMethod.GET, "/api/incomes/by-type?incomeType=Salary", user, null);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        assertThat(json(response)).hasSize(1);
    }

    private void expectStatus(SoftAssertions softly, HttpMethod method, String path, ApiUser user, Object body, int status) {
        ResponseEntity<String> response = call(method, path, user, body);
        softly.assertThat(response.getStatusCode().value()).as(method + " " + path + " -> " + response.getBody()).isEqualTo(status);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/actuator/health", "/actuator/health/liveness", "/actuator/prometheus",
            "/swagger-ui/index.html"})
    void publicEndpoint_noToken_isReachable(String path) {
        ResponseEntity<String> response = call(HttpMethod.GET, path, null, null);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
    }

    @Test
    void health_anonymous_hidesComponentDetails() {
        assertThat(json(call(HttpMethod.GET, "/actuator/health", null, null)).has("components")).isFalse();
    }

    @Disabled("BUG-13: springdoc 2.6.0 is incompatible with Spring 6.2, so /v3/api-docs throws NoSuchMethodError")
    @Test
    void openApiDocs_noToken_returns200() {
        assertThat(call(HttpMethod.GET, "/v3/api-docs", null, null).getStatusCode().value()).isEqualTo(200);
    }

    @Disabled("BUG-13: springdoc 2.6.0 is incompatible with Spring 6.2, so /v3/api-docs throws NoSuchMethodError")
    @Test
    void openApiDocs_declareBearerAuth() {
        assertThat(json(call(HttpMethod.GET, "/v3/api-docs", null, null))
                .at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
    }

    @Test
    void actuatorEnv_authenticatedUser_isNotExposed() {
        assertThat(call(HttpMethod.GET, "/actuator/env", newUser(), null).getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    void cors_allowedOrigin_preflightSucceedsWithCredentials() {
        ResponseEntity<String> response = preflight(ALLOWED_ORIGIN, "POST");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED_ORIGIN);
        assertThat(response.getHeaders().getAccessControlAllowCredentials()).isTrue();
    }

    @Test
    void cors_unknownOrigin_preflightIsRefused() {
        ResponseEntity<String> response = preflight("https://evil.example", "POST");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void cors_methodOutsideAllowList_preflightIsRefused() {
        assertThat(preflight(ALLOWED_ORIGIN, "PATCH").getStatusCode().value()).isEqualTo(403);
    }

    private ResponseEntity<String> preflight(String origin, String requestedMethod) {
        return call(HttpMethod.OPTIONS, "/api/incomes", null, null,
                Map.of(HttpHeaders.ORIGIN, origin, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, requestedMethod));
    }
}
