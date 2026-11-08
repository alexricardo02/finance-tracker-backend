package com.example.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataIsolationIT extends AbstractApiIntegrationTest {

    private ApiUser alice;
    private ApiUser bob;
    private int aliceIncome;
    private int aliceExpense;

    @BeforeEach
    void seed() {
        alice = newUser();
        bob = newUser();
        aliceIncome = createIncome(alice, 100, "alice-secret-income");
        aliceExpense = createExpense(alice, 10, "alice-secret-expense");
    }

    private double storedIncomeAmount() {
        return jdbc.queryForObject("SELECT amount FROM incomes WHERE income_id = ?", Double.class, aliceIncome);
    }

    private double storedExpenseAmount() {
        return jdbc.queryForObject("SELECT amount FROM expenses WHERE id = ?", Double.class, aliceExpense);
    }

    private void assertRefusedWithoutData(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getBody()).isFalse();
        assertThat(response.getStatusCode().is5xxServerError()).as(response.getBody()).isFalse();
        assertThat(response.getBody() == null ? "" : response.getBody()).doesNotContain("alice-secret");
    }

    @Test
    void lists_onlyContainCallersTransactions() {
        assertThat(call(HttpMethod.GET, "/api/incomes", bob, null).getBody()).doesNotContain("alice-secret");
        assertThat(call(HttpMethod.GET, "/api/expenses", bob, null).getBody()).doesNotContain("alice-secret");
        assertThat(call(HttpMethod.GET, "/api/incomes", alice, null).getBody()).contains("alice-secret-income");
    }

    @Test
    void totals_doNotIncludeOtherUsersAmounts() {
        assertThat(call(HttpMethod.GET, "/api/incomes/total-month?month=JULY", bob, null).getBody()).isEqualTo("0.0");
        assertThat(call(HttpMethod.GET, "/api/incomes/total-month?month=JULY", alice, null).getBody()).isEqualTo("100.0");
    }

    @Test
    void export_doesNotContainOtherUsersTransactions() {
        assertThat(call(HttpMethod.GET, "/api/exports/transactions?format=csv", bob, null).getBody())
                .doesNotContain("alice-secret");
        assertThat(call(HttpMethod.GET, "/api/exports/transactions?format=csv", alice, null).getBody())
                .contains("alice-secret-income", "alice-secret-expense");
    }

    @Test
    void getIncomeById_otherUser_isRefused() {
        assertRefusedWithoutData(call(HttpMethod.GET, "/api/incomes/" + aliceIncome, bob, null));
    }

    @Test
    void getIncomeById_otherUser_returns403() {
        assertThat(call(HttpMethod.GET, "/api/incomes/" + aliceIncome, bob, null).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void updateIncome_otherUser_isRefusedAndRowUnchanged() {
        assertRefusedWithoutData(call(HttpMethod.PUT, "/api/incomes/" + aliceIncome, bob, incomeBody(999, "USD", TX_DATE, "hacked")));
        assertThat(storedIncomeAmount()).isEqualTo(100.0);
    }

    @Test
    void updateExpense_otherUser_isRefusedAndRowUnchanged() {
        assertRefusedWithoutData(call(HttpMethod.PUT, "/api/expenses/" + aliceExpense, bob, expenseBody(999, "USD", TX_DATE, "hacked")));
        assertThat(storedExpenseAmount()).isEqualTo(10.0);
    }

    @Test
    void updateTransaction_otherUser_returns403() {
        assertThat(call(HttpMethod.PUT, "/api/incomes/" + aliceIncome, bob, incomeBody(999, "USD", TX_DATE, "hacked"))
                .getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.PUT, "/api/expenses/" + aliceExpense, bob, expenseBody(999, "USD", TX_DATE, "hacked"))
                .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void deleteTransactions_otherUser_isRefusedAndNotSoftDeleted() {
        assertRefusedWithoutData(call(HttpMethod.DELETE, "/api/incomes/" + aliceIncome, bob, null));
        assertRefusedWithoutData(call(HttpMethod.DELETE, "/api/expenses/" + aliceExpense, bob, null));

        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM incomes WHERE income_id = ?", Boolean.class, aliceIncome)).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM expenses WHERE id = ?", Boolean.class, aliceExpense)).isTrue();
    }

    @Test
    void deleteTransaction_otherUser_returns403() {
        assertThat(call(HttpMethod.DELETE, "/api/incomes/" + aliceIncome, bob, null).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void categories_otherUsersPrivateCategoryIsHidden() {
        createCategory(alice, "alice-secret-category", "income");

        assertThat(call(HttpMethod.GET, "/api/categories", bob, null).getBody()).doesNotContain("alice-secret-category");
    }

    @Test
    void deleteCategory_otherUsersPrivateCategory_returns403AndKeepsIt() {
        int aliceCategory = createCategory(alice, "alice-secret-category", "income");

        assertThat(call(HttpMethod.DELETE, "/api/categories/" + aliceCategory, bob, null).getStatusCode().value()).isEqualTo(403);
        assertThat(categoryRepository.existsById(aliceCategory)).isTrue();
    }

    @Test
    void createTransaction_withOtherUsersPrivateCategory_isRefused() {
        int aliceCategory = createCategory(alice, "alice-secret-category", "income");
        Map<String, Object> income = Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "categoryId", aliceCategory, "paymentMethod", "CASH");

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", bob, income);

        assertRefusedWithoutData(response);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, bob.id())).isZero();
    }

    @Test
    void transactions_withOwnPrivateCategory_areAccepted() {
        int aliceCategory = createCategory(alice, "alice-own", "expense");
        Map<String, Object> expense = Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "categoryId", aliceCategory, "paymentMethod", "CASH");

        assertThat(call(HttpMethod.POST, "/api/expenses", alice, expense).getStatusCode().value()).isEqualTo(201);
        assertThat(call(HttpMethod.PUT, "/api/expenses/" + aliceExpense, alice, expense).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void createExpenseAndUpdateIncome_withOtherUsersPrivateCategory_areRefused() {
        int aliceCategory = createCategory(alice, "alice-secret-category", "expense");
        int bobIncome = createIncome(bob, 5, "bob");
        Map<String, Object> body = Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "categoryId", aliceCategory, "paymentMethod", "CASH");

        assertThat(call(HttpMethod.POST, "/api/expenses", bob, body).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.PUT, "/api/incomes/" + bobIncome, bob, body).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void updateTransaction_withOtherUsersPrivateCategory_isRefused() {
        int aliceCategory = createCategory(alice, "alice-secret-category", "expense");
        int bobExpense = createExpense(bob, 5, "bob");
        Map<String, Object> expense = Map.of("amount", 1.0, "currency", "USD", "date", TX_DATE,
                "categoryId", aliceCategory, "paymentMethod", "CASH");

        ResponseEntity<String> response = call(HttpMethod.PUT, "/api/expenses/" + bobExpense, bob, expense);

        assertRefusedWithoutData(response);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT category_id FROM expenses WHERE id = ?", Integer.class, bobExpense))
                .isEqualTo(globalCategoryId("Food"));
    }
}
