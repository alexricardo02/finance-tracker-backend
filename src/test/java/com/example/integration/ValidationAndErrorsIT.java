package com.example.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ValidationAndErrorsIT extends AbstractApiIntegrationTest {

    private Map<String, Object> without(Map<String, Object> body, String field) {
        Map<String, Object> copy = new HashMap<>(body);
        copy.remove(field);
        return copy;
    }

    @Test
    void getIncomes_pageAndSize_returnSliceWithMetadataNewestFirst() {
        ApiUser user = newUser();
        for (int day = 1; day <= 5; day++) {
            call(HttpMethod.POST, "/api/incomes", user, incomeBody(day, "USD", "2026-07-0" + day, "d" + day));
        }

        JsonNode page = json(call(HttpMethod.GET, "/api/incomes?page=1&size=2", user, null));

        assertThat(page.get("pageNo").asInt()).isEqualTo(1);
        assertThat(page.get("pageSize").asInt()).isEqualTo(2);
        assertThat(page.get("totalElements").asInt()).isEqualTo(5);
        assertThat(page.get("totalPages").asInt()).isEqualTo(3);
        assertThat(page.get("last").asBoolean()).isFalse();
        assertThat(page.get("content").findValuesAsText("date")).containsExactly("2026-07-03", "2026-07-02");
    }

    @Test
    void getExpenses_lastPage_isFlaggedAndBeyondLastIsEmpty() {
        ApiUser user = newUser();
        createExpense(user, 1, "a");
        createExpense(user, 2, "b");

        assertThat(json(call(HttpMethod.GET, "/api/expenses?page=0&size=2", user, null)).get("last").asBoolean()).isTrue();
        assertThat(json(call(HttpMethod.GET, "/api/expenses?page=5&size=2", user, null)).get("content")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=-5"})
    void getTransactions_invalidPaging_returns400(String query) {
        ApiUser user = newUser();

        assertErrorShape(call(HttpMethod.GET, "/api/incomes?" + query, user, null), 400);
        assertErrorShape(call(HttpMethod.GET, "/api/expenses?" + query, user, null), 400);
    }

    @Test
    void getTransactions_dateRange_isInclusiveAndExcludesOutside() {
        ApiUser user = newUser();
        call(HttpMethod.POST, "/api/expenses", user, expenseBody(1, "USD", "2026-06-30", "before"));
        call(HttpMethod.POST, "/api/expenses", user, expenseBody(2, "USD", "2026-07-01", "start"));
        call(HttpMethod.POST, "/api/expenses", user, expenseBody(3, "USD", "2026-07-31", "end"));
        call(HttpMethod.POST, "/api/expenses", user, expenseBody(4, "USD", "2026-08-01", "after"));

        JsonNode content = json(call(HttpMethod.GET, "/api/expenses?startDate=2026-07-01&endDate=2026-07-31", user, null)).get("content");

        assertThat(content.findValuesAsText("description")).containsExactly("end", "start");
    }

    @Test
    void getTransactions_categoryAndPaymentFilters_narrowResults() {
        ApiUser user = newUser();
        int mine = createCategory(user, "Mine", "income");
        call(HttpMethod.POST, "/api/incomes", user, Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "description", "match", "categoryId", mine, "paymentMethod", "DEBIT_CARD"));
        createIncome(user, 2, "other");

        assertThat(json(call(HttpMethod.GET, "/api/incomes?categoryId=" + mine, user, null)).get("content")
                .findValuesAsText("description")).containsExactly("match");
        assertThat(json(call(HttpMethod.GET, "/api/incomes?paymentMethod=DEBIT_CARD", user, null)).get("content")
                .findValuesAsText("description")).containsExactly("match");
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency", "date", "categoryId", "paymentMethod"})
    void createIncome_missingRequiredField_returns400(String field) {
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, without(incomeBody(1, "USD", TX_DATE, "x"), field)), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"date", "categoryId", "paymentMethod"})
    void createExpense_missingRequiredField_returns400(String field) {
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/expenses", user, without(expenseBody(1, "USD", TX_DATE, "x"), field)), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency"})
    void createExpense_missingAmountOrCurrency_returns400(String field) {
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/expenses", user, without(expenseBody(1, "USD", TX_DATE, "x"), field)), 400);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 0})
    void createTransaction_nonPositiveAmount_returns400(double amount) {
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(amount, "USD", TX_DATE, "x")), 400);
        assertErrorShape(call(HttpMethod.POST, "/api/expenses", user, expenseBody(amount, "USD", TX_DATE, "x")), 400);
    }

    @Test
    void createTransaction_unsupportedCurrency_returns400() {
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, "USD&symbols=X", TX_DATE, "x")), 400);
        assertErrorShape(call(HttpMethod.POST, "/api/expenses", user, expenseBody(1, "NOPE", TX_DATE, "x")), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"username\":\"\",\"password\":\"Passw0rd!\",\"email\":\"a@test.com\"}",
            "{\"username\":\"weakpw\",\"password\":\"short\",\"email\":\"a@test.com\"}",
            "{\"username\":\"noemail\",\"password\":\"Passw0rd!\",\"email\":\"\"}"})
    void register_invalidField_returns400(String body) {
        assertErrorShape(call(HttpMethod.POST, "/api/users/register", null, body), 400);
    }

    @Test
    void register_malformedEmail_returns400() {
        assertErrorShape(call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", "bademail" + System.nanoTime(), "password", PASSWORD, "email", "not-an-email")), 400);
    }

    @Test
    void forgotPassword_malformedEmail_returns400() {
        assertErrorShape(call(HttpMethod.POST, "/api/users/forgot-password", null, Map.of("email", "not-an-email")), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"primaryCurrency\":\"XXX\"}", "{\"primaryCurrency\":\"\"}"})
    void updateCurrency_unsupportedValue_returns400(String body) {
        assertErrorShape(call(HttpMethod.PUT, "/api/settings/currency", newUser(), body), 400);
    }

    @Test
    void createCategory_missingFields_returns400() {
        assertErrorShape(call(HttpMethod.POST, "/api/categories", newUser(), "{}"), 400);
    }

    @Test
    void export_unsupportedFormat_returns400WithoutInjectingHeaders() {
        ResponseEntity<String> response = call(HttpMethod.GET, "/api/exports/transactions?format=exe", newUser(), null);

        assertErrorShape(response, 400);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).isNull();
    }

    @Test
    void totalMonth_invalidMonth_returns400() {
        assertErrorShape(call(HttpMethod.GET, "/api/incomes/total-month?month=NOPE", newUser(), null), 400);
    }

    @Test
    void importTransactions_validRows_createsBothKindsAndNewCategory() {
        ApiUser user = newUser();
        String rows = "[{\"kind\":\"income\",\"amount\":5.0,\"currency\":\"USD\",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"Salary\"},"
                + "{\"kind\":\"expense\",\"amount\":6.0,\"currency\":\"USD\",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"BrandNew\"}]";

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/imports/transactions", user, rows);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(json(response).get("imported").asInt()).isEqualTo(2);
        assertThat(call(HttpMethod.GET, "/api/categories", user, null).getBody()).contains("BrandNew");
    }

    static Stream<Arguments> invalidImports() {
        String row = "{\"kind\":\"income\",\"amount\":1.0,\"currency\":\"USD\",\"date\":\"2026-07-01\",\"categoryName\":\"Salary\"}";
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < 2001; i++) {
            tooMany.add(row);
        }
        return Stream.of(
                arguments("empty list", "[]"),
                arguments("over 2000 rows", "[" + String.join(",", tooMany) + "]"),
                arguments("invalid kind", "[" + row.replace("income", "bonus") + "]"),
                arguments("negative amount", "[" + row.replace("1.0", "-1.0") + "]"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidImports")
    void importTransactions_invalidPayload_returns400(String name, String body) {
        assertErrorShape(call(HttpMethod.POST, "/api/imports/transactions", newUser(), body), 400);
    }

    @Test
    void malformedJson_returns400() {
        assertErrorShape(call(HttpMethod.POST, "/api/incomes", newUser(), "{not json"), 400);
    }

    @Test
    void unknownEnumValue_returns400() {
        ApiUser user = newUser();
        Map<String, Object> body = new HashMap<>(incomeBody(1, "USD", TX_DATE, "x"));
        body.put("paymentMethod", "BITCOIN");

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, body), 400);
    }

    @Test
    void nonNumericPathId_returns400() {
        assertErrorShape(call(HttpMethod.GET, "/api/incomes/abc", newUser(), null), 400);
    }

    @Test
    void malformedDateFilter_returns400() {
        assertErrorShape(call(HttpMethod.GET, "/api/incomes?startDate=yesterday", newUser(), null), 400);
    }

    @Test
    void missingRequiredQueryParam_returns400() {
        assertErrorShape(call(HttpMethod.GET, "/api/incomes/total-month", newUser(), null), 400);
    }

    @Test
    void unsupportedHttpMethod_returns405() {
        assertErrorShape(call(HttpMethod.DELETE, "/api/incomes", newUser(), null), 405);
    }

    @Test
    void unknownUrl_returns404() {
        assertErrorShape(call(HttpMethod.GET, "/api/does-not-exist", newUser(), null), 404);
    }

    @Test
    void missingTransaction_returns404() {
        ApiUser user = newUser();

        assertErrorShape(call(HttpMethod.GET, "/api/incomes/99999999", user, null), 404);
    }

    @Test
    void updateOrDeleteMissingTransaction_returns404() {
        ApiUser user = newUser();

        assertErrorShape(call(HttpMethod.DELETE, "/api/incomes/99999999", user, null), 404);
        assertErrorShape(call(HttpMethod.DELETE, "/api/expenses/99999999", user, null), 404);
        assertErrorShape(call(HttpMethod.PUT, "/api/incomes/99999999", user, incomeBody(1, "USD", TX_DATE, "x")), 404);
        assertErrorShape(call(HttpMethod.PUT, "/api/expenses/99999999", user, expenseBody(1, "USD", TX_DATE, "x")), 404);
    }

    @Test
    void missingCategory_returns404() {
        ApiUser user = newUser();
        Map<String, Object> body = new HashMap<>(incomeBody(1, "USD", TX_DATE, "x"));
        body.put("categoryId", 99999999);

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, body), 404);
    }

    @Test
    void deleteCategory_globalOrMissing_returnExpectedStatus() {
        ApiUser user = newUser();

        assertErrorShape(call(HttpMethod.DELETE, "/api/categories/" + globalCategoryId("Food"), user, null), 403);
        assertErrorShape(call(HttpMethod.DELETE, "/api/categories/99999999", user, null), 404);
    }

    @Test
    void deleteCategory_inUse_returns409() {
        ApiUser user = newUser();
        int inUse = createCategory(user, "Mine", "expense");
        call(HttpMethod.POST, "/api/expenses", user, Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "categoryId", inUse, "paymentMethod", "CASH"));

        assertErrorShape(call(HttpMethod.DELETE, "/api/categories/" + inUse, user, null), 409);
    }

    @Test
    void dataConstraintViolation_returns409WithoutSqlDetails() {
        // A soft-deleted account is invisible to the existence checks but still holds the unique username,
        // so only the database rejects the second registration.
        ApiUser deleted = newUser();
        call(HttpMethod.DELETE, "/api/users/me", deleted, Map.of("password", PASSWORD));

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", deleted.username(), "password", PASSWORD, "email", System.nanoTime() + "@test.com"));

        assertMessage(response, 409, "The operation violates a data constraint (invalid or duplicate reference)");
        assertThat(response.getBody()).doesNotContainIgnoringCase("duplicate key").doesNotContain("users", "uk", "SQL");
    }

    @Test
    void createCategory_nameOver100Characters_returns400() {
        assertMessage(call(HttpMethod.POST, "/api/categories", newUser(), Map.of("name", "x".repeat(101), "type", "income")),
                400, "Category name must be at most 100 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", "USD'; --"})
    void invalidCurrency_rejectedOnEveryWritePath_beforeReachingFxApi(String currency) throws Exception {
        ApiUser user = newUser();
        String importRow = "[{\"kind\":\"income\",\"amount\":1.0,\"currency\":" + objectMapper.writeValueAsString(currency)
                + ",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"Salary\"}]";

        assertMessage(call(HttpMethod.POST, "/api/expenses", user, expenseBody(1, currency, TX_DATE, "x")), 400, "Unsupported currency");
        assertMessage(call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, currency, TX_DATE, "x")), 400, "Unsupported currency");
        assertMessage(call(HttpMethod.POST, "/api/imports/transactions", user, importRow), 400, "Unsupported currency");
        assertMessage(call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", currency)), 400, "Unsupported currency");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, user.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses WHERE user_id = ?", Integer.class, user.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT primary_currency FROM users WHERE user_id = ?", String.class, user.id())).isEqualTo("USD");
        assertThat(FX_API.getAllServeEvents()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/incomes", "/api/expenses"})
    void listTransactions_sizeAtLimit_returns200(String path) {
        ApiUser user = newUser();

        assertThat(call(HttpMethod.GET, path + "?size=1000", user, null).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET, path + "?page=0&size=100", user, null).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET, path, user, null).getStatusCode().value()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/incomes", "/api/expenses"})
    void listTransactions_paginationOutOfRange_returns400(String path) {
        ApiUser user = newUser();

        assertMessage(call(HttpMethod.GET, path + "?size=1001", user, null), 400, "Size must be between 1 and 1000");
        assertMessage(call(HttpMethod.GET, path + "?size=0", user, null), 400, "Size must be between 1 and 1000");
        assertMessage(call(HttpMethod.GET, path + "?page=-1", user, null), 400, "Page must be 0 or greater");
    }

    @Test
    void register_usernameWithQuote_returns400() {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", "bad\"name" + System.nanoTime(), "password", PASSWORD, "email", System.nanoTime() + "@test.com"));

        assertMessage(response, 400, "Username must be 3-50 characters: letters, digits, '.', '_' or '-'");
    }

    @Test
    void login_legacyUsernameOutsideNewPattern_stillWorks() {
        ApiUser legacy = newUser("legacy user@" + System.nanoTime());

        assertThat(login(legacy.username(), PASSWORD).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createCategory_invalidType_returns400() {
        assertMessage(call(HttpMethod.POST, "/api/categories", newUser(), Map.of("name", "Mine", "type", "Income")),
                400, "Category type must be 'income' or 'expense'");
    }

    @Test
    void createTransaction_descriptionOver255Characters_returns400() {
        ApiUser user = newUser();
        String description = "d".repeat(256);

        assertMessage(call(HttpMethod.POST, "/api/incomes", user, incomeBody(1, "USD", TX_DATE, description)),
                400, "Description must be at most 255 characters");
        assertMessage(call(HttpMethod.POST, "/api/expenses", user, expenseBody(1, "USD", TX_DATE, description)),
                400, "Description must be at most 255 characters");
    }

    private void assertMessage(ResponseEntity<String> response, int status, String message) {
        assertErrorShape(response, status);
        assertThat(json(response).get("message").asText()).isEqualTo(message);
    }
}
