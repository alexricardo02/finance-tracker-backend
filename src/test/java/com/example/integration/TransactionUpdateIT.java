package com.example.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionUpdateIT extends AbstractApiIntegrationTest {

    @Test
    void updateIncome_newCurrency_storesCurrencyAndConvertsAmount() {
        ApiUser user = newUser();
        stubFrankfurter("EUR", "USD", TX_DATE, 2.0);
        int income = createIncome(user, 100, "x");

        JsonNode updated = json(call(HttpMethod.PUT, "/api/incomes/" + income, user, incomeBody(50, "EUR", TX_DATE, "x")));

        assertThat(updated.get("currency").asText()).isEqualTo("EUR");
        assertThat(updated.get("amountPrimaryCurrency").asDouble()).isEqualTo(100.0);
    }

    @Disabled("BUG-9: updateExpense does not store the new currency")
    @Test
    void updateExpense_newCurrency_storesCurrencyAndConvertsAmount() {
        ApiUser user = newUser();
        stubFrankfurter("EUR", "USD", TX_DATE, 2.0);
        int expense = createExpense(user, 100, "x");

        JsonNode updated = json(call(HttpMethod.PUT, "/api/expenses/" + expense, user, expenseBody(50, "EUR", TX_DATE, "x")));

        assertThat(updated.get("currency").asText()).isEqualTo("EUR");
        assertThat(updated.get("amountPrimaryCurrency").asDouble()).isEqualTo(100.0);
    }
}
