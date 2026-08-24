package com.example.events;

import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.User;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.service.CacheService;
import com.example.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimaryCurrencyChangedListenerTest {

    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private CacheService cacheService;

    @InjectMocks private PrimaryCurrencyChangedListener listener;

    private User user;
    private Income income;
    private Expense expense;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);

        income = new Income();
        income.setIncomeId(10);
        income.setAmount(100.0);
        income.setCurrency("USD");
        income.setDate(LocalDate.of(2026, 6, 1));
        income.setUser(user);

        Category expenseCategory = new Category("Food", "expense", user);
        expense = new Expense();
        expense.setExpenseID(20);
        expense.setExpenseAmount(50.0);
        expense.setCurrency("USD");
        expense.setExpenseDate(LocalDate.of(2026, 6, 2));
        expense.setCategory(expenseCategory);
        expense.setUser(user);
    }

    // WHY helpers: findByUserUserId returns Page<T>, not List<T>. Using PageImpl
    // keeps stubs honest with the actual repository contract.
    private static <T> Page<T> pageOf(List<T> items) {
        return new PageImpl<>(items);
    }

    @Test
    void recalculate_recomputesAmountPrimaryCurrency_usingEachTransactionOwnDate() {
        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(1, "john", "EUR");

        when(incomeRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of(income)));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of(expense)));
        when(exchangeRateService.getConversionRate("USD", "EUR", LocalDate.of(2026, 6, 1))).thenReturn(0.9);
        when(exchangeRateService.getConversionRate("USD", "EUR", LocalDate.of(2026, 6, 2))).thenReturn(0.9);

        listener.recalculate(event);

        assertThat(income.getAmountPrimaryCurrency()).isEqualTo(90.0);
        assertThat(expense.getAmountPrimaryCurrency()).isEqualTo(45.0);
        verify(incomeRepository).save(income);
        verify(expenseRepository).save(expense);
    }

    @Test
    void recalculate_evictsUserFinancialCache_afterRecalculation() {
        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(1, "john", "GBP");

        when(incomeRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));

        listener.recalculate(event);

        verify(cacheService).evictUserFinancialCache("john");
    }

    @Test
    void recalculate_noTransactions_doesNotFailAndStillEvictsCache() {
        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(1, "john", "JPY");

        when(incomeRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));

        listener.recalculate(event);

        verifyNoInteractions(exchangeRateService);
        verify(cacheService).evictUserFinancialCache("john");
    }

    @Test
    void onPrimaryCurrencyChanged_delegatesToRecalculate_withSameEvent() {
        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(1, "john", "ARS");
        when(incomeRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));

        listener.onPrimaryCurrencyChanged(event);

        verify(cacheService).evictUserFinancialCache("john");
    }

    @Test
    void recalculate_multipleIncomesForUser_eachConvertedWithOwnRate() {
        Income income2 = new Income();
        income2.setIncomeId(11);
        income2.setAmount(200.0);
        income2.setCurrency("USD");
        income2.setDate(LocalDate.of(2026, 5, 1));
        income2.setUser(user);

        PrimaryCurrencyChangedEvent event = new PrimaryCurrencyChangedEvent(1, "john", "EUR");
        when(incomeRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of(income, income2)));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(pageOf(List.of()));
        when(exchangeRateService.getConversionRate("USD", "EUR", LocalDate.of(2026, 6, 1))).thenReturn(0.9);
        when(exchangeRateService.getConversionRate("USD", "EUR", LocalDate.of(2026, 5, 1))).thenReturn(0.85);

        listener.recalculate(event);

        assertThat(income.getAmountPrimaryCurrency()).isEqualTo(90.0);
        assertThat(income2.getAmountPrimaryCurrency()).isEqualTo(170.0);
        verify(incomeRepository, times(2)).save(any(Income.class));
    }
}
