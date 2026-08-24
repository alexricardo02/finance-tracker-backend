package com.example.service;

import com.example.dataTransferObjects.ImportResultDTO;
import com.example.dataTransferObjects.TransactionImportRowDTO;
import com.example.models.Category;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private CacheService cacheService;

    @InjectMocks private ImportService importService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
        user.setPrimaryCurrency("USD");
    }

    private TransactionImportRowDTO row(String kind, double amount, String category) {
        TransactionImportRowDTO row = new TransactionImportRowDTO();
        row.setKind(kind);
        row.setAmount(amount);
        row.setCurrency("USD");
        row.setDate(LocalDate.of(2026, 7, 1));
        row.setCategoryName(category);
        row.setPaymentMethod(PaymentMethod.OTHER);
        return row;
    }

    @Test
    void importTransactions_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.importTransactions(List.of(), "ghost"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void importTransactions_mixedValidRows_savesBothKindsAndEvictsCache() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(categoryRepository.findByUserUserIdOrUserIsNull(1)).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exchangeRateService.getConversionRate(anyString(), anyString(), any())).thenReturn(1.0);

        List<TransactionImportRowDTO> rows = List.of(
                row("income", 1000.0, "Salary"),
                row("expense", 50.0, "Food")
        );

        ImportResultDTO result = importService.importTransactions(rows, "john");

        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getSkipped()).isZero();
        verify(incomeRepository).saveAll(anyList());
        verify(expenseRepository).saveAll(anyList());
        verify(cacheService).evictUserFinancialCache("john");
    }

    @Test
    void importTransactions_conversionRateFails_rowSkipped_othersStillImported() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(categoryRepository.findByUserUserIdOrUserIsNull(1)).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exchangeRateService.getConversionRate(anyString(), anyString(), any()))
                .thenReturn(1.0)
                .thenThrow(new RuntimeException("FX service unavailable"));

        List<TransactionImportRowDTO> rows = List.of(
                row("income", 1000.0, "Salary"),
                row("expense", 50.0, "Food")
        );

        ImportResultDTO result = importService.importTransactions(rows, "john");

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Row 2"));
    }

    @Test
    void importTransactions_existingCategory_isReused_notRecreated() {
        Category existing = new Category("Food", "expense", user);
        existing.setCategoryId(9);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(categoryRepository.findByUserUserIdOrUserIsNull(1)).thenReturn(List.of(existing));
        when(exchangeRateService.getConversionRate(anyString(), anyString(), any())).thenReturn(1.0);

        importService.importTransactions(List.of(row("expense", 30.0, "Food")), "john");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void importTransactions_missingCategoryName_defaultsToUncategorized() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(categoryRepository.findByUserUserIdOrUserIsNull(1)).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            assertThat(c.getName()).isEqualTo("Uncategorized");
            return c;
        });
        when(exchangeRateService.getConversionRate(anyString(), anyString(), any())).thenReturn(1.0);

        importService.importTransactions(List.of(row("income", 100.0, "")), "john");

        verify(categoryRepository).save(any(Category.class));
    }
}