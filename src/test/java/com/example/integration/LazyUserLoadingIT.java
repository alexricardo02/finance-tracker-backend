package com.example.integration;

import com.example.service.ExpenseService;
import com.example.service.IncomeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class LazyUserLoadingIT extends AbstractApiIntegrationTest {

    @Autowired private IncomeService incomeService;
    @Autowired private ExpenseService expenseService;

    @Test
    void getExpenseById_outsideCallerTransaction_loadsOwner() {
        ApiUser user = newUser();
        int expense = createExpense(user, 10, "x");

        assertThat(expenseService.getExpenseById(expense, user.username()).getPrimaryCurrency()).isEqualTo("USD");
    }

    @Test
    void paginatedReads_outsideCallerTransaction_loadOwner() {
        ApiUser user = newUser();
        createIncome(user, 100, "x");
        createExpense(user, 10, "y");

        assertThat(incomeService.getIncomesForCurrentUserPaginated(user.username(), 0, 10).getContent())
                .singleElement().satisfies(income -> assertThat(income.getPrimaryCurrency()).isEqualTo("USD"));
        assertThat(expenseService.getExpensesForCurrentUserPaginated(user.username(), 0, 10).getContent())
                .singleElement().satisfies(expense -> assertThat(expense.getPrimaryCurrency()).isEqualTo("USD"));
    }
}
