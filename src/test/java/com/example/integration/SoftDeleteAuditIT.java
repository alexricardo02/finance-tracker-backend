package com.example.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.sql.Timestamp;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SoftDeleteAuditIT extends AbstractApiIntegrationTest {

    @Test
    void deleteIncome_hidesRowButKeepsItWithAuditFields() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "to-delete");

        assertThat(call(HttpMethod.DELETE, "/api/incomes/" + income, user, null).getStatusCode().value()).isEqualTo(204);

        assertThat(call(HttpMethod.GET, "/api/incomes", user, null).getBody()).doesNotContain("to-delete");
        assertThat(call(HttpMethod.GET, "/api/incomes/total-month?month=JULY", user, null).getBody()).isEqualTo("0.0");
        assertThat(call(HttpMethod.GET, "/api/exports/transactions?format=csv", user, null).getBody()).doesNotContain("to-delete");
        assertThat(call(HttpMethod.GET, "/api/incomes/" + income, user, null).getStatusCode().value()).isEqualTo(404);
        Map<String, Object> row = jdbc.queryForMap("SELECT deleted_at, deleted_by FROM incomes WHERE income_id = ?", income);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("deleted_by")).isEqualTo(user.username());
    }

    @Test
    void deleteExpense_hidesRowButKeepsItWithAuditFields() {
        ApiUser user = newUser();
        int expense = createExpense(user, 10, "to-delete");

        assertThat(call(HttpMethod.DELETE, "/api/expenses/" + expense, user, null).getStatusCode().value()).isEqualTo(204);

        assertThat(call(HttpMethod.GET, "/api/expenses", user, null).getBody()).doesNotContain("to-delete");
        assertThat(call(HttpMethod.GET, "/api/exports/transactions?format=csv", user, null).getBody()).doesNotContain("to-delete");
        Map<String, Object> row = jdbc.queryForMap("SELECT deleted_at, deleted_by FROM expenses WHERE id = ?", expense);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("deleted_by")).isEqualTo(user.username());
    }

    @Disabled("BUG-8: client errors are not mapped to 4xx by GlobalExceptionHandler")
    @Test
    void softDeletedTransaction_cannotBeDeletedOrUpdatedAgain() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        int expense = createExpense(user, 10, "x");
        call(HttpMethod.DELETE, "/api/incomes/" + income, user, null);
        call(HttpMethod.DELETE, "/api/expenses/" + expense, user, null);

        assertErrorShape(call(HttpMethod.DELETE, "/api/incomes/" + income, user, null), 404);
        assertErrorShape(call(HttpMethod.DELETE, "/api/expenses/" + expense, user, null), 404);
        assertErrorShape(call(HttpMethod.PUT, "/api/incomes/" + income, user, incomeBody(1, "USD", TX_DATE, "y")), 404);
        assertErrorShape(call(HttpMethod.PUT, "/api/expenses/" + expense, user, expenseBody(1, "USD", TX_DATE, "y")), 404);
    }

    @Test
    void deleteCategory_usedOnlyBySoftDeletedTransaction_doesNotFailWithServerError() {
        ApiUser user = newUser();
        int category = createCategory(user, "Mine", "income");
        int income = json(call(HttpMethod.POST, "/api/incomes", user, Map.of("amount", 1.0, "currency", "USD",
                "date", TX_DATE, "categoryId", category, "paymentMethod", "CASH"))).get("incomeId").asInt();
        call(HttpMethod.DELETE, "/api/incomes/" + income, user, null);

        assertThat(call(HttpMethod.DELETE, "/api/categories/" + category, user, null).getStatusCode().value()).isIn(204, 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE income_id = ?", Integer.class, income)).isEqualTo(1);
    }

    @Test
    void createAndUpdate_populateAuditTimestamps() {
        ApiUser user = newUser();
        int income = createIncome(user, 100, "x");
        int expense = createExpense(user, 10, "x");
        Timestamp incomeCreated = jdbc.queryForObject("SELECT created_at FROM incomes WHERE income_id = ?", Timestamp.class, income);
        Timestamp incomeUpdated = jdbc.queryForObject("SELECT updated_at FROM incomes WHERE income_id = ?", Timestamp.class, income);
        Timestamp expenseCreated = jdbc.queryForObject("SELECT created_at FROM expenses WHERE id = ?", Timestamp.class, expense);
        assertThat(incomeCreated).isNotNull();
        assertThat(incomeUpdated).isNotNull();
        assertThat(expenseCreated).isNotNull();

        call(HttpMethod.PUT, "/api/incomes/" + income, user, incomeBody(200, "USD", TX_DATE, "changed"));
        call(HttpMethod.PUT, "/api/expenses/" + expense, user, expenseBody(20, "USD", TX_DATE, "changed"));

        assertThat(jdbc.queryForObject("SELECT created_at FROM incomes WHERE income_id = ?", Timestamp.class, income)).isEqualTo(incomeCreated);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM incomes WHERE income_id = ?", Timestamp.class, income)).isAfter(incomeUpdated);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM expenses WHERE id = ?", Timestamp.class, expense)).isAfter(expenseCreated);
    }

    @Test
    void deleteAccount_deactivatesUserAndHardDeletesData() {
        ApiUser user = newUser();
        createIncome(user, 100, "x");
        int softDeleted = createExpense(user, 10, "y");
        call(HttpMethod.DELETE, "/api/expenses/" + softDeleted, user, null);

        assertThat(call(HttpMethod.DELETE, "/api/users/me", user, Map.of("password", PASSWORD)).getStatusCode().value()).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE user_id = ?", Boolean.class, user.id())).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, user.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses WHERE user_id = ?", Integer.class, user.id())).isZero();
        assertThat(login(user.username(), PASSWORD).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.GET, "/api/settings", user, null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void deleteAccount_wrongPassword_keepsEverything() {
        ApiUser user = newUser();
        createIncome(user, 100, "x");

        assertErrorShape(call(HttpMethod.DELETE, "/api/users/me", user, Map.of("password", "WrongPass1")), 401);

        assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE user_id = ?", Boolean.class, user.id())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, user.id())).isEqualTo(1);
    }

    @Test
    void reRegister_afterAccountDeletion_doesNotFailWithServerError() {
        ApiUser user = newUser();
        call(HttpMethod.DELETE, "/api/users/me", user, Map.of("password", PASSWORD));

        int status = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", user.username(), "password", PASSWORD, "email", user.email())).getStatusCode().value();

        assertThat(status).isIn(201, 400, 409);
    }
}
