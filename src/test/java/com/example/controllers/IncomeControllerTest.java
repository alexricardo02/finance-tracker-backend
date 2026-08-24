package com.example.controllers;

import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.models.PaymentMethod;
import com.example.service.IncomeService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeControllerTest {

    @Mock private IncomeService incomeService;

    @InjectMocks private IncomeController incomeController;

    private Principal principal;
    private IncomeResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        principal = () -> "john";

        sampleResponse = new IncomeResponseDTO();
        sampleResponse.setIncomeId(20);
        sampleResponse.setAmount(1500.0);
        sampleResponse.setCurrency("USD");
        sampleResponse.setDate(LocalDate.of(2026, 7, 1));
    }

    // ── createIncome ──────────────────────────────────────────────────────────

    @Test
    void createIncome_validRequest_delegatesToServiceAndReturnsDTO() {
        IncomeRequestDTO request = new IncomeRequestDTO();
        request.setAmount(1500.0);
        request.setCurrency("USD");
        request.setDate(LocalDate.of(2026, 7, 1));
        request.setCategoryId(3);
        request.setUserId(1);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        when(incomeService.saveIncome(request, "john")).thenReturn(sampleResponse);

        IncomeResponseDTO result = incomeController.createIncome(principal, request);

        assertThat(result.getIncomeId()).isEqualTo(20);
        assertThat(result.getAmount()).isEqualTo(1500.0);
        verify(incomeService).saveIncome(request, "john");
    }

    // ── getAllIncomes ──────────────────────────────────────────────────────────

    @Test
    void getAllIncomes_noFilters_returnsPagedResponse() {
        PagedResponse<IncomeResponseDTO> paged = new PagedResponse<>(
                List.of(sampleResponse), 0, 1000, 1L, 1, true);
        when(incomeService.getFilteredIncomes("john", null, null, null, null, 0, 1000))
                .thenReturn(paged);

        ResponseEntity<PagedResponse<IncomeResponseDTO>> response =
                incomeController.getAllIncomes(principal, 0, 1000, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(incomeService).getFilteredIncomes("john", null, null, null, null, 0, 1000);
    }

    @Test
    void getAllIncomes_withPaymentMethodFilter_passesFilterToService() {
        PagedResponse<IncomeResponseDTO> paged = new PagedResponse<>(List.of(), 0, 1000, 0L, 0, true);
        when(incomeService.getFilteredIncomes("john", null, null, null, PaymentMethod.CASH, 0, 1000))
                .thenReturn(paged);

        incomeController.getAllIncomes(principal, 0, 1000, null, null, null, PaymentMethod.CASH);

        verify(incomeService).getFilteredIncomes("john", null, null, null, PaymentMethod.CASH, 0, 1000);
    }

    // ── getIncomeById ─────────────────────────────────────────────────────────

    @Test
    void getIncomeById_ownedByRequester_returnsDTO() {
        when(incomeService.getIncomeById(20, "john")).thenReturn(sampleResponse);

        IncomeResponseDTO result = incomeController.getIncomeById(principal, 20);

        assertThat(result.getIncomeId()).isEqualTo(20);
        verify(incomeService).getIncomeById(20, "john");
    }

    @Test
    void getIncomeById_ownedByAnotherUser_serviceThrowsSecurityException() {
        // WHY: IDOR check — service enforces ownership so controller must pass principal.
        when(incomeService.getIncomeById(20, "john"))
                .thenThrow(new SecurityException("No tienes permiso para ver este ingreso"));

        assertThatThrownBy(() -> incomeController.getIncomeById(principal, 20))
                .isInstanceOf(SecurityException.class);
    }

    // ── updateIncome ──────────────────────────────────────────────────────────

    @Test
    void updateIncome_ownedByRequester_returnsUpdatedDTO() {
        IncomeRequestDTO request = new IncomeRequestDTO();
        request.setAmount(2000.0);
        request.setCurrency("USD");
        request.setDate(LocalDate.of(2026, 8, 1));
        request.setCategoryId(3);
        request.setUserId(1);
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        IncomeResponseDTO updated = new IncomeResponseDTO();
        updated.setIncomeId(20);
        updated.setAmount(2000.0);
        when(incomeService.updateIncome(20, request, "john")).thenReturn(updated);

        IncomeResponseDTO result = incomeController.updateIncome(principal, 20, request);

        assertThat(result.getAmount()).isEqualTo(2000.0);
        verify(incomeService).updateIncome(20, request, "john");
    }

    // ── deleteIncome ──────────────────────────────────────────────────────────

    @Test
    void deleteIncome_ownedByRequester_callsServiceWithPrincipalUsername() {
        doNothing().when(incomeService).deleteIncome(20, "john");

        incomeController.deleteIncome(principal, 20);

        verify(incomeService).deleteIncome(20, "john");
    }

    @Test
    void deleteIncome_ownedByAnotherUser_serviceThrowsSecurityException() {
        doThrow(new SecurityException("No tienes permiso para eliminar este ingreso"))
                .when(incomeService).deleteIncome(20, "john");

        assertThatThrownBy(() -> incomeController.deleteIncome(principal, 20))
                .isInstanceOf(SecurityException.class);
    }

    // ── by-type ───────────────────────────────────────────────────────────────

    @Test
    void getIncomesByType_delegatesToService_returnsList() {
        when(incomeService.findByIncomeTypeName("Salary", "john")).thenReturn(List.of(sampleResponse));

        var result = incomeController.getIncomesByType(principal, "Salary", null);

        assertThat(result).hasSize(1);
        verify(incomeService).findByIncomeTypeName("Salary", "john");
    }

    // ── total-month ───────────────────────────────────────────────────────────

    @Test
    void getTotalByMonth_delegatesToService_returnsDouble() {
        when(incomeService.getTotalIncomeAmountByMonth("JULY", "john")).thenReturn(3000.0);

        Double result = incomeController.getTotalByMonth(principal, "JULY");

        assertThat(result).isEqualTo(3000.0);
        verify(incomeService).getTotalIncomeAmountByMonth("JULY", "john");
    }
}
