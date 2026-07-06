package com.example.service;

import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.models.Category;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.IncomeRepository;
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
class IncomeServiceTest {
	
	/**
    @Mock private CategoryRepository categoryRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private UserRepository userRepository;
    @Mock private CacheService cacheService;
    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks private IncomeService incomeService;

    private User user;
    private Category category;
    private Income income;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new java.util.Date());
        user.setUser_id(1);

        category = new Category();
        category.setCategoryId(6);
        category.setName("Salary");

        income = new Income();
        income.setIncomeId(10);
        income.setAmount(233.0);
        income.setCurrency("USD");
        income.setDate(LocalDate.of(2026, 7, 5));
        income.setCategory(category);
        income.setUser(user);
        income.setPaymentMethod(PaymentMethod.DEBIT_CARD);
    }

    @Test
    void saveIncome_success() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(233.0);
        dto.setCurrency("USD");
        dto.setDate(LocalDate.of(2026, 7, 5));
        dto.setCategoryId(6);
        dto.setPaymentMethod(PaymentMethod.DEBIT_CARD);

        when(categoryRepository.findById(6)).thenReturn(Optional.of(category));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        
        // FIX: Prevenimos el NullPointerException devolviendo una tasa fija
        when(exchangeRateService.getConversionRate(any(), any(), any())).thenReturn(1.0);
        
        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        IncomeResponseDTO result = incomeService.saveIncome(dto, "john");

        assertThat(result.getIncomeId()).isEqualTo(10);
        assertThat(result.getAmount()).isEqualTo(233.0);
    }

    @Test
    void saveIncome_amountNull_throwsIllegalArgumentException() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(null);

        assertThatThrownBy(() -> incomeService.saveIncome(dto, "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveIncome_categoryNotFound_throwsResponseStatusException() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(100.0);
        dto.setCategoryId(999);

        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.saveIncome(dto, "john"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getIncomeById_success() {
        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));

        IncomeResponseDTO result = incomeService.getIncomeById(10, "john");

        assertThat(result.getIncomeId()).isEqualTo(10);
    }

    @Test
    void getIncomeById_notFound_throwsEntityNotFoundException() {
        when(incomeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.getIncomeById(999, "john"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void getIncomeById_wrongUser_throwsSecurityException() {
        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));

        assertThatThrownBy(() -> incomeService.getIncomeById(10, "someoneElse"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteIncome_success_softDeletes() {
        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));

        incomeService.deleteIncome(10, "john");

        verify(incomeRepository, times(1)).save(income);
        assertThat(income.getDeletedAt()).isNotNull();
        assertThat(income.getDeletedBy()).isEqualTo("john");
    }

    @Test
    void deleteIncome_wrongUser_throwsSecurityException() {
        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));

        assertThatThrownBy(() -> incomeService.deleteIncome(10, "intruder"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteIncome_notFound_throwsRuntimeException() {
        when(incomeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.deleteIncome(999, "john"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateIncome_success() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        dto.setAmount(500.0);
        dto.setDate(LocalDate.of(2026, 8, 1));
        dto.setDescription("updated");
        dto.setCurrency("EUR");
        dto.setCategoryId(6);
        dto.setPaymentMethod(PaymentMethod.CASH);

        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));
        when(categoryRepository.findById(6)).thenReturn(Optional.of(category));
        
        // FIX: Prevenimos el NullPointerException en la actualización
        when(exchangeRateService.getConversionRate(any(), any(), any())).thenReturn(1.0);
        
        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        IncomeResponseDTO result = incomeService.updateIncome(10, dto, "john");

        assertThat(result).isNotNull();
    }

    @Test
    void updateIncome_wrongUser_throwsSecurityException() {
        IncomeRequestDTO dto = new IncomeRequestDTO();
        when(incomeRepository.findById(10)).thenReturn(Optional.of(income));

        assertThatThrownBy(() -> incomeService.updateIncome(10, dto, "intruder"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getTotalIncomeAmountByMonth_validMonth_returnsAmount() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(incomeRepository.getTotalIncomeAmountByMonthAndUser(7, 1)).thenReturn(1000.0);

        Double result = incomeService.getTotalIncomeAmountByMonth("JULY", "john");

        assertThat(result).isEqualTo(1000.0);
    }

    @Test
    void getTotalIncomeAmountByMonth_invalidMonth_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> incomeService.getTotalIncomeAmountByMonth("NOTAMONTH", "john"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTotalIncomesLast7DaysInclusive_nullResult_returnsZero() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(incomeRepository.getTotalIncomeAmountBetweenAndUser(any(), any(), eq(1))).thenReturn(null);

        Double result = incomeService.getTotalIncomesLast7DaysInclusive(LocalDate.now(), "john");

        assertThat(result).isEqualTo(0.0);
    }
    **/
}