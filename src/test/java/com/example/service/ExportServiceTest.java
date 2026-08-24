package com.example.service;

import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ExportService exportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
        user.setPrimaryCurrency("USD");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
    }

    @Test
    void export_csv_incomeOnly_containsHeaderAndRow() throws Exception {
        Category category = new Category("Salary", "income", user);
        Income income = new Income();
        income.setIncomeId(1);
        income.setAmount(500.0);
        income.setCurrency("USD");
        income.setDate(LocalDate.of(2026, 7, 1));
        income.setCategory(category);
        income.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        income.setAmountPrimaryCurrency(500.0);
        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(income));

        byte[] result = exportService.export("john", "csv", null, null, null, null, "INCOME");
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Type,Category,Description,Payment Method,Amount,Currency,Amount (Primary),Primary Currency");
        assertThat(csv).contains("INCOME").contains("Salary").contains("500.0");
        verifyNoInteractions(expenseRepository);
    }

    @Test
    void export_csv_expenseOnly_excludesIncomes() throws Exception {
        Category category = new Category("Food", "expense", user);
        Expense expense = new Expense();
        expense.setExpenseID(2);
        expense.setExpenseAmount(75.0);
        expense.setCurrency("USD");
        expense.setExpenseDate(LocalDate.of(2026, 7, 2));
        expense.setCategory(category);
        expense.setPaymentMethod(PaymentMethod.CASH);
        expense.setAmountPrimaryCurrency(75.0);
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        byte[] result = exportService.export("john", "csv", null, null, null, null, "EXPENSE");
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).contains("EXPENSE").contains("Food");
        assertThat(csv).doesNotContain("INCOME");
        verifyNoInteractions(incomeRepository);
    }

    @Test
    void export_csv_descriptionWithComma_isQuotedAndEscaped() throws Exception {
        Category category = new Category("Food", "expense", user);
        Expense expense = new Expense();
        expense.setExpenseID(3);
        expense.setExpenseAmount(10.0);
        expense.setCurrency("USD");
        expense.setExpenseDate(LocalDate.of(2026, 7, 3));
        expense.setCategory(category);
        expense.setExpenseDescription("lunch, with client");
        expense.setPaymentMethod(PaymentMethod.CASH);
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        byte[] result = exportService.export("john", "csv", null, null, null, null, "EXPENSE");
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).contains("\"lunch, with client\"");
    }

    @Test
    void export_csv_bothKinds_sortedByDateDescending() throws Exception {
        Category incomeCat = new Category("Salary", "income", user);
        Income income = new Income();
        income.setIncomeId(1);
        income.setAmount(500.0);
        income.setCurrency("USD");
        income.setDate(LocalDate.of(2026, 7, 1));
        income.setCategory(incomeCat);
        income.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        Category expenseCat = new Category("Food", "expense", user);
        Expense expense = new Expense();
        expense.setExpenseID(2);
        expense.setExpenseAmount(20.0);
        expense.setCurrency("USD");
        expense.setExpenseDate(LocalDate.of(2026, 7, 5));
        expense.setCategory(expenseCat);
        expense.setPaymentMethod(PaymentMethod.CASH);

        when(incomeRepository.findAll(any(Specification.class))).thenReturn(List.of(income));
        when(expenseRepository.findAll(any(Specification.class))).thenReturn(List.of(expense));

        byte[] result = exportService.export("john", "csv", null, null, null, null, "ALL");
        String csv = new String(result, StandardCharsets.UTF_8);
        String[] lines = csv.split("\n");

        assertThat(lines[1]).contains("2026-07-05"); // most recent expense first
        assertThat(lines[2]).contains("2026-07-01");
    }
}