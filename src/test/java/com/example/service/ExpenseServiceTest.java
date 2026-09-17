package com.example.service;

import com.example.dataTransferObjects.ExpenseRequestDTO;
import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.PaymentMethod;
import com.example.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CacheService cacheService;
    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks private ExpenseService expenseService;

    private User user;
    private Expense expense;
    private Category category;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(expenseService, "meterRegistry", new SimpleMeterRegistry());

        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);

        category = new Category("Food", "expense", user);
        category.setCategoryId(1);

        expense = new Expense(20, 150.0, "USD", LocalDate.of(2026, 7, 5), category, "Expense", "lunch", PaymentMethod.DEBIT_CARD);
        expense.setUser(user);
    }

    @Test
    void saveExpense_success() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(150.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.of(2026, 7, 5));
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(exchangeRateService.getConversionRate(any(), any(), any())).thenReturn(1.0);
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponseDTO result = expenseService.saveExpense(dto, "john");

        assertThat(result.getAmount()).isEqualTo(150.0);
        verify(cacheService).evictUserFinancialCache("john");
        verify(cacheService).evictGlobalCache("all_expenses_types", "all");
    }

    @Test
    void saveExpense_nullRequestDTO_throwsResponseStatusException() {
        assertThatThrownBy(() -> expenseService.saveExpense(null, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void saveExpense_nullCategoryId_throwsResponseStatusException() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setCategoryId(null);

        assertThatThrownBy(() -> expenseService.saveExpense(dto, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void saveExpense_categoryNotFound_throwsResponseStatusException() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setCategoryId(999);

        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.saveExpense(dto, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getExpenseById_success() {
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        ExpenseResponseDTO result = expenseService.getExpenseById(20, "john");

        assertThat(result).isNotNull();
    }

    @Test
    void getExpenseById_notFound_throwsIllegalArgumentException() {
        when(expenseRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(999, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getExpenseById_wrongUser_throwsSecurityException() {
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.getExpenseById(20, "intruder"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteExpense_success_softDeletes() {
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(20, "john");

        verify(expenseRepository, times(1)).save(expense);
        assertThat(expense.getDeletedAt()).isNotNull();
        verify(cacheService).evictUserFinancialCache("john");
        verify(cacheService).evictGlobalCache("all_expenses_types", "all");
    }

    @Test
    void deleteExpense_wrongUser_throwsSecurityException() {
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.deleteExpense(20, "intruder"))
                .isInstanceOf(SecurityException.class);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updateExpense_success() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(200.0);
        dto.setDate(LocalDate.of(2026, 8, 1));
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);
        dto.setDescription("updated");
        dto.setCurrency("USD");

        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(exchangeRateService.getConversionRate(any(), any(), any())).thenReturn(1.0);
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponseDTO result = expenseService.updateExpense(20, dto, "john");

        assertThat(result).isNotNull();
    }

    @Test
    void updateExpense_wrongUser_throwsSecurityException() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.updateExpense(20, dto, "intruder"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getTotalExpenseAmounByMonthAndUser_invalidMonth_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> expenseService.getTotalExpenseAmounByMonthAndUser("NOTAMONTH", "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalExpenseAmounByYearAndUser_nullYear_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> expenseService.getTotalExpenseAmounByYearAndUser(null, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalExpenseAmounByDayAndUser_invalidDay_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> expenseService.getTotalExpenseAmounByDayAndUser(50, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalExpenseAmounByDayAndUser_nullDay_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> expenseService.getTotalExpenseAmounByDayAndUser(null, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalExpenseAmounByDayAndUser_dayBelowOne_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> expenseService.getTotalExpenseAmounByDayAndUser(0, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalExpenseAmounByDayAndUser_validDay_returnsAmount() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountByDayAndUser(15, 1)).thenReturn(75.0);

        Double result = expenseService.getTotalExpenseAmounByDayAndUser(15, "john");

        assertThat(result).isEqualTo(75.0);
    }

    @Test
    void getTotalExpenseAmounByMonthAndUser_validMonth_returnsAmount() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountByMonthAndUser(7, 1)).thenReturn(500.0);

        Double result = expenseService.getTotalExpenseAmounByMonthAndUser("JULY", "john");

        assertThat(result).isEqualTo(500.0);
    }

    @Test
    void getTotalExpenseAmounByYearAndUser_validYear_returnsAmount() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountByYearAndUser(2026, 1)).thenReturn(5000.0);

        Double result = expenseService.getTotalExpenseAmounByYearAndUser(2026, "john");

        assertThat(result).isEqualTo(5000.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFilteredExpenses_returnsPagedResponse() {
        Page<Expense> page = new PageImpl<>(List.of(expense), PageRequest.of(0, 10), 1);
        when(expenseRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResponse<ExpenseResponseDTO> result = expenseService.getFilteredExpenses(
                "john", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1L, PaymentMethod.DEBIT_CARD, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getExpensesForCurrentUserPaginated_success() {
        Page<Expense> page = new PageImpl<>(List.of(expense), PageRequest.of(0, 10), 1);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.findByUserUserId(eq(1), any(Pageable.class))).thenReturn(page);

        PagedResponse<ExpenseResponseDTO> result = expenseService.getExpensesForCurrentUserPaginated("john", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(20);
    }

    @Test
    void getExpensesForCurrentUserPaginated_userNotFound_throwsRuntimeException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpensesForCurrentUserPaginated("ghost", 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void getTotalExpenseAmountByTypeAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountByTypeAndUser("Food", 1)).thenReturn(300.0);

        Double result = expenseService.getTotalExpenseAmountByTypeAndUser("Food", "john");

        assertThat(result).isEqualTo(300.0);
    }

    @Test
    void getTotalExpensesLast7DaysInclusiveAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(150.0);

        Double result = expenseService.getTotalExpensesLast7DaysInclusiveAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(150.0);
    }

    @Test
    void getTotalExpensesLast7DaysInclusiveAndUser_nullReturnsZero() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(null);

        Double result = expenseService.getTotalExpensesLast7DaysInclusiveAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void getTotalExpensesLastMonthsAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(450.0);

        Double result = expenseService.getTotalExpensesLastMonthsAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(450.0);
    }

    @Test
    void getTotalExpensesLast3MonthsAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(900.0);

        Double result = expenseService.getTotalExpensesLast3MonthsAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(900.0);
    }

    @Test
    void getTotalExpensesLast6MonthsAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(1800.0);

        Double result = expenseService.getTotalExpensesLast6MonthsAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(1800.0);
    }

    @Test
    void getTotalExpensesLastYearAndUser_returnsValue() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(expenseRepository.getTotalExpenseAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(3600.0);

        Double result = expenseService.getTotalExpensesLastYearAndUser(LocalDate.now(), "john");

        assertThat(result).isEqualTo(3600.0);
    }
}