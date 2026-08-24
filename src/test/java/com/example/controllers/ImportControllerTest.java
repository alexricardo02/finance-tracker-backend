package com.example.controllers;

import com.example.dataTransferObjects.ImportResultDTO;
import com.example.dataTransferObjects.TransactionImportRowDTO;
import com.example.models.PaymentMethod;
import com.example.service.ImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Mock private ImportService importService;

    @InjectMocks private ImportController importController;

    private final Principal principal = () -> "john";

    private TransactionImportRowDTO validRow(String kind) {
        TransactionImportRowDTO row = new TransactionImportRowDTO();
        row.setKind(kind);
        row.setAmount(100.0);
        row.setCurrency("USD");
        row.setDate(LocalDate.of(2026, 7, 1));
        row.setCategoryName("Salary");
        row.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        return row;
    }

    @Test
    void importTransactions_validRows_delegatesToServiceAndReturnsResult() {
        List<TransactionImportRowDTO> rows = List.of(validRow("income"), validRow("expense"));
        ImportResultDTO result = new ImportResultDTO(2, 0, List.of());
        when(importService.importTransactions(rows, "john")).thenReturn(result);

        ImportResultDTO response = importController.importTransactions(principal, rows);

        assertThat(response.getImported()).isEqualTo(2);
        assertThat(response.getSkipped()).isEqualTo(0);
        verify(importService).importTransactions(rows, "john");
    }

    @Test
    void importTransactions_partialFailures_reportedInErrors() {
        List<TransactionImportRowDTO> rows = List.of(validRow("income"));
        ImportResultDTO result = new ImportResultDTO(0, 1, List.of("Row 1: category not found"));
        when(importService.importTransactions(rows, "john")).thenReturn(result);

        ImportResultDTO response = importController.importTransactions(principal, rows);

        assertThat(response.getSkipped()).isEqualTo(1);
        assertThat(response.getErrors()).containsExactly("Row 1: category not found");
    }

    @Test
    void importTransactions_passesPrincipalUsernameToService() {
        // WHY: The controller must always pass principal.getName(), never a userId
        // from the request body, to prevent privilege escalation.
        Principal alice = () -> "alice";
        List<TransactionImportRowDTO> rows = List.of(validRow("income"));
        when(importService.importTransactions(rows, "alice"))
                .thenReturn(new ImportResultDTO(1, 0, List.of()));

        importController.importTransactions(alice, rows);

        verify(importService).importTransactions(rows, "alice");
    }
}
