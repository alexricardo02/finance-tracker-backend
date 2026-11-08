package com.example.integration;

import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Native aggregates use Postgres EXTRACT and COALESCE, which is why this runs against the real engine.
@Transactional
class TransactionRepositoryIT extends AbstractIntegrationTest {

    private static final LocalDate JUL_15 = LocalDate.of(2026, 7, 15);
    private static final LocalDate AUG_20 = LocalDate.of(2027, 8, 20);

    @Autowired private IncomeRepository incomes;
    @Autowired private ExpenseRepository expenses;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private JdbcTemplate jdbc;
    @PersistenceContext private EntityManager entityManager;

    private User user;
    private User other;
    private Category category;

    @BeforeEach
    void seed() {
        user = users.saveAndFlush(newUser());
        other = users.saveAndFlush(newUser());
        category = categories.saveAndFlush(new Category("Bonus-" + UUID.randomUUID(), "income", user));
    }

    private User newUser() {
        String name = UUID.randomUUID().toString();
        return new User(name, name + "@test.com", "hash", new Date());
    }

    private Income income(User owner, double amount, Double primary, LocalDate date, Instant deletedAt) {
        Income income = new Income();
        income.setUser(owner);
        income.setAmount(amount);
        income.setAmountPrimaryCurrency(primary);
        income.setCurrency("USD");
        income.setDate(date);
        income.setCategory(category);
        income.setPaymentMethod(PaymentMethod.CASH);
        income.setDeletedAt(deletedAt);
        return incomes.saveAndFlush(income);
    }

    private Expense expense(User owner, double amount, Double primary, LocalDate date, Instant deletedAt) {
        Expense expense = new Expense();
        expense.setUser(owner);
        expense.setExpenseAmount(amount);
        expense.setAmountPrimaryCurrency(primary);
        expense.setCurrency("USD");
        expense.setExpenseDate(date);
        expense.setCategory(category);
        expense.setPaymentMethod(PaymentMethod.CASH);
        expense.setDeletedAt(deletedAt);
        return expenses.saveAndFlush(expense);
    }

    @Test
    void incomeTotals_preferPrimaryAmountAndFallBackToRawAmount() {
        income(user, 100, 50.0, JUL_15, null);
        income(user, 30, null, JUL_15, null);

        assertThat(incomes.getTotalIncomeAmountByMonthAndUser(7, user.getUserId())).isEqualTo(80.0);
    }

    @Test
    void incomeTotals_excludeSoftDeletedAndOtherUsers() {
        income(user, 100, 100.0, JUL_15, null);
        income(user, 500, 500.0, JUL_15, Instant.now());
        income(other, 900, 900.0, JUL_15, null);

        int id = user.getUserId();
        assertThat(incomes.getTotalIncomeAmountByMonthAndUser(7, id)).isEqualTo(100.0);
        assertThat(incomes.getTotalIncomeAmountByYearAndUser(2026, id)).isEqualTo(100.0);
        assertThat(incomes.getTotalIncomeAmountByDayAndUser(15, id)).isEqualTo(100.0);
        assertThat(incomes.getTotalIncomeAmountBetweenAndUser(JUL_15, JUL_15, id)).isEqualTo(100.0);
        assertThat(incomes.getTotalIncomeAmountByTypeAndUser(category.getName(), id)).isEqualTo(100.0);
        assertThat(incomes.findByIncomeTypeNameAndUser(category.getName(), id)).hasSize(1);
    }

    @Test
    void incomeTotals_noMatchingRows_returnZeroNotNull() {
        int id = user.getUserId();

        assertThat(incomes.getTotalIncomeAmountByMonthAndUser(1, id)).isZero();
        assertThat(incomes.getTotalIncomeAmountBetweenAndUser(JUL_15, JUL_15, id)).isZero();
    }

    @Test
    void incomeTotals_extractMatchesOnlyRequestedMonthYearAndDay() {
        income(user, 100, 100.0, JUL_15, null);
        income(user, 7, 7.0, AUG_20, null);

        int id = user.getUserId();
        assertThat(incomes.getTotalIncomeAmountByMonthAndUser(8, id)).isEqualTo(7.0);
        assertThat(incomes.getTotalIncomeAmountByYearAndUser(2027, id)).isEqualTo(7.0);
        assertThat(incomes.getTotalIncomeAmountByDayAndUser(20, id)).isEqualTo(7.0);
    }

    @Test
    void expenseTotals_excludeSoftDeletedAndOtherUsers() {
        expense(user, 100, 100.0, JUL_15, null);
        expense(user, 40, null, JUL_15, null);
        expense(user, 500, 500.0, JUL_15, Instant.now());
        expense(other, 900, 900.0, JUL_15, null);

        int id = user.getUserId();
        assertThat(expenses.getTotalExpenseAmountByMonthAndUser(7, id)).isEqualTo(140.0);
        assertThat(expenses.getTotalExpenseAmountByYearAndUser(2026, id)).isEqualTo(140.0);
        assertThat(expenses.getTotalExpenseAmountByDayAndUser(15, id)).isEqualTo(140.0);
        assertThat(expenses.getTotalExpenseAmountBetweenAndUser(JUL_15, JUL_15, id)).isEqualTo(140.0);
    }

    @Disabled("BUG-18: expense by-type queries filter on expenses.type, which the entity never populates")
    @Test
    void expenseTotalByType_matchesTheCategoryOfTheExpense() {
        expense(user, 100, 100.0, JUL_15, null);

        assertThat(expenses.getTotalExpenseAmountByTypeAndUser(category.getName(), user.getUserId())).isEqualTo(100.0);
        assertThat(expenses.findByExpenseTypeNameAndUser(category.getName(), user.getUserId())).hasSize(1);
    }

    @Test
    void entityQueries_hideSoftDeletedRows() {
        Income active = income(user, 1, 1.0, JUL_15, null);
        Income deleted = income(user, 2, 2.0, JUL_15, Instant.now());
        Expense deletedExpense = expense(user, 2, 2.0, JUL_15, Instant.now());
        // The persistence context would otherwise serve these rows and bypass @Where.
        entityManager.clear();

        assertThat(incomes.findById(active.getIncomeId())).isPresent();
        assertThat(incomes.findById(deleted.getIncomeId())).isEmpty();
        assertThat(expenses.findById(deletedExpense.getExpenseID())).isEmpty();
        assertThat(incomes.findByUserUsername(user.getUsername())).hasSize(1);
        assertThat(incomes.existsByCategory_CategoryId(category.getCategoryId())).isTrue();
    }

    @Test
    void paging_sortsByDateDescendingAndCountsOnlyActiveRows() {
        income(user, 1, 1.0, JUL_15, null);
        income(user, 2, 2.0, AUG_20, null);
        income(user, 3, 3.0, AUG_20, Instant.now());

        var page = incomes.findByUserUserId(user.getUserId(), PageRequest.of(0, 1, Sort.by("date").descending()));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getDate()).isEqualTo(AUG_20);
    }

    @Test
    void hardDeleteAll_removesSoftDeletedRowsToo_andOnlyForThatUser() {
        income(user, 1, 1.0, JUL_15, null);
        income(user, 2, 2.0, JUL_15, Instant.now());
        income(other, 3, 3.0, JUL_15, null);
        expense(user, 1, 1.0, JUL_15, Instant.now());

        incomes.hardDeleteAllByUserId(user.getUserId());
        expenses.hardDeleteAllByUserId(user.getUserId());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, user.getUserId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses WHERE user_id = ?", Integer.class, user.getUserId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM incomes WHERE user_id = ?", Integer.class, other.getUserId())).isEqualTo(1);
    }
}
