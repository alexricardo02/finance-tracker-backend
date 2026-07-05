package com.example.service;

import com.example.dataTransferObjects.ExpenseRequestDTO;
import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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

    @InjectMocks private ExpenseService expenseService;

    private User user;
    private Expense expense;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new java.util.Date());
        user.setUser_id(1);

        // WHY: Schema normalization replaced string-based types with a relational Category entity
        category = new Category("Food", "Expense", user);
        category.setCategoryId(1);

        // WHY: The Expense constructor now requires a Category object and a PaymentMethod enum instead of raw strings
        expense = new Expense(20, 150.0, "USD", LocalDate.of(2026, 7, 5), category, "Expense", "lunch", PaymentMethod.DEBIT_CARD);
        expense.setUser(user);
    }

    @Test
    void saveExpense_success() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(150.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.of(2026, 7, 5));
        
        // WHY: ExpenseRequestDTO uses categoryId and paymentMethod now, typeName was removed
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.DEBIT_CARD);
        dto.setUserId(1);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        // WHY: ExpenseService.saveExpense fetches the Category from DB before saving
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseResponseDTO result = expenseService.saveExpense(dto, "john");

        assertThat(result.getAmount()).isEqualTo(150.0);
    }

    @Test
    void saveExpense_nullRequestDTO_throwsResponseStatusException() {
        assertThatThrownBy(() -> expenseService.saveExpense(null, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void saveExpense_nullUserId_throwsResponseStatusException() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setUserId(null);

        assertThatThrownBy(() -> expenseService.saveExpense(dto, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void saveExpense_nullCategoryId_throwsResponseStatusException() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(100.0);
        dto.setUserId(1);
        dto.setCategoryId(null);

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
    }

    @Test
    void deleteExpense_wrongUser_throwsSecurityException() {
        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.deleteExpense(20, "intruder"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateExpense_success() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setAmount(200.0);
        dto.setDate(LocalDate.of(2026, 8, 1));
        
        // WHY: Aligning test payload with updated DTO structure
        dto.setCategoryId(1);
        dto.setPaymentMethod(PaymentMethod.CASH);
        dto.setDescription("updated");

        when(expenseRepository.findById(20)).thenReturn(Optional.of(expense));
        // WHY: updateExpense also validates/fetches the new Category from DB
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
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
}