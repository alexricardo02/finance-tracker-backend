package com.example.controllers;

import com.example.dataTransferObjects.ExpenseRequestDTO;
import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.models.PaymentMethod;
import com.example.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock private ExpenseService expenseService;

    @InjectMocks private ExpenseController expenseController;

    private Principal principal;
    private ExpenseResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        principal = () -> "john";

        sampleResponse = new ExpenseResponseDTO();
        sampleResponse.setId(10);
        sampleResponse.setAmount(99.50);
        sampleResponse.setCurrency("USD");
        sampleResponse.setDate(LocalDate.of(2026, 7, 1));
    }

    // ── createExpense ─────────────────────────────────────────────────────────

    @Test
    void createExpense_validRequest_returnsCreatedResponseDTO() {
        ExpenseRequestDTO request = new ExpenseRequestDTO();
        request.setAmount(99.50);
        request.setDate(LocalDate.of(2026, 7, 1));
        request.setCategoryId(1);
        request.setPaymentMethod(PaymentMethod.CASH);

        when(expenseService.saveExpense(request, "john")).thenReturn(sampleResponse);

        ExpenseResponseDTO result = expenseController.createExpense(principal, request);

        assertThat(result.getId()).isEqualTo(10);
        assertThat(result.getAmount()).isEqualTo(99.50);
        verify(expenseService).saveExpense(request, "john");
    }

    // ── getAllExpenses ─────────────────────────────────────────────────────────

    @Test
    void getAllExpenses_noFilters_returnsPagedResponse() {
        PagedResponse<ExpenseResponseDTO> paged = new PagedResponse<>(
                List.of(sampleResponse), 0, 1000, 1L, 1, true);
        when(expenseService.getFilteredExpenses("john", null, null, null, null, 0, 1000))
                .thenReturn(paged);

        ResponseEntity<PagedResponse<ExpenseResponseDTO>> response =
                expenseController.getAllExpenses(principal, 0, 1000, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getAllExpenses_withDateRangeAndCategoryFilters_passesFiltersToService() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 6, 30);
        Long categoryId = 5L;

        PagedResponse<ExpenseResponseDTO> paged = new PagedResponse<>(List.of(), 0, 1000, 0L, 0, true);
        when(expenseService.getFilteredExpenses("john", start, end, categoryId, null, 0, 1000))
                .thenReturn(paged);

        expenseController.getAllExpenses(principal, 0, 1000, start, end, categoryId, null);

        verify(expenseService).getFilteredExpenses("john", start, end, categoryId, null, 0, 1000);
    }

    // ── updateExpense ─────────────────────────────────────────────────────────

    @Test
    void updateExpense_ownedByRequester_returnsUpdatedDTO() {
        ExpenseRequestDTO request = new ExpenseRequestDTO();
        request.setAmount(200.0);
        request.setDate(LocalDate.of(2026, 8, 1));
        request.setCategoryId(2);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        ExpenseResponseDTO updated = new ExpenseResponseDTO();
        updated.setId(10);
        updated.setAmount(200.0);
        when(expenseService.updateExpense(10, request, "john")).thenReturn(updated);

        ExpenseResponseDTO result = expenseController.updateExpense(principal, 10, request);

        assertThat(result.getAmount()).isEqualTo(200.0);
        verify(expenseService).updateExpense(10, request, "john");
    }

    @Test
    void updateExpense_ownedByAnotherUser_serviceThrowsSecurityException() {
        // WHY: IDOR — the service is the enforcement point. The controller passes
        // principal.getName() so the service can check ownership.
        ExpenseRequestDTO request = new ExpenseRequestDTO();
        request.setAmount(50.0);
        request.setDate(LocalDate.of(2026, 8, 1));
        request.setCategoryId(1);
        request.setPaymentMethod(PaymentMethod.CASH);

        when(expenseService.updateExpense(10, request, "john"))
                .thenThrow(new SecurityException("No tienes permiso para editar este gasto"));

        assertThatThrownBy(() -> expenseController.updateExpense(principal, 10, request))
                .isInstanceOf(SecurityException.class);
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    void deleteExpense_ownedByRequester_callsServiceWithPrincipalUsername() {
        doNothing().when(expenseService).deleteExpense(10, "john");

        expenseController.deleteExpense(principal, 10);

        verify(expenseService).deleteExpense(10, "john");
    }

    @Test
    void deleteExpense_ownedByAnotherUser_serviceThrowsSecurityException() {
        // WHY: same IDOR pattern as update — ownership is enforced at the service layer.
        doThrow(new SecurityException("No tienes permiso para eliminar este egreso"))
                .when(expenseService).deleteExpense(10, "john");

        assertThatThrownBy(() -> expenseController.deleteExpense(principal, 10))
                .isInstanceOf(SecurityException.class);
    }
}
