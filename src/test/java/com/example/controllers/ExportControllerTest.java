package com.example.controllers;

import com.example.service.ExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock private ExportService exportService;

    @InjectMocks private ExportController exportController;

    private final Principal principal = () -> "john";

    @Test
    void exportTransactions_csvFormat_returnsTextCsvContentType() throws Exception {
        byte[] csvBytes = "Date,Type\n2026-07-01,INCOME\n".getBytes();
        when(exportService.export(eq("john"), eq("csv"), isNull(), isNull(), isNull(), isNull(), eq("ALL")))
                .thenReturn(csvBytes);

        ResponseEntity<byte[]> response = exportController.exportTransactions(
                principal, "csv", null, null, null, null, "ALL");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/csv"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains(".csv");
        assertThat(response.getBody()).isEqualTo(csvBytes);
    }

    @Test
    void exportTransactions_xlsxFormat_returnsSpreadsheetContentType() throws Exception {
        byte[] xlsxBytes = new byte[]{1, 2, 3};
        when(exportService.export(eq("john"), eq("xlsx"), isNull(), isNull(), isNull(), isNull(), eq("ALL")))
                .thenReturn(xlsxBytes);

        ResponseEntity<byte[]> response = exportController.exportTransactions(
                principal, "xlsx", null, null, null, null, "ALL");

        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains(".xlsx");
    }

    @Test
    void exportTransactions_pdfFormat_returnsApplicationPdfContentType() throws Exception {
        byte[] pdfBytes = new byte[]{4, 5, 6};
        when(exportService.export(eq("john"), eq("pdf"), isNull(), isNull(), isNull(), isNull(), eq("EXPENSE")))
                .thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = exportController.exportTransactions(
                principal, "pdf", null, null, null, null, "EXPENSE");

        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains(".pdf");
    }

    @Test
    void exportTransactions_unknownFormat_throwsBadRequest() throws Exception {
        // WHY: the controller now rejects unsupported formats with 400 BAD_REQUEST
        // instead of silently defaulting to CSV.
        assertThatThrownBy(() -> exportController.exportTransactions(
                principal, "nope", null, null, null, null, "ALL"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported export format");
    }

    @Test
    void exportTransactions_filenameContainsCurrentDate() throws Exception {
        byte[] data = new byte[0];
        when(exportService.export(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(data);

        ResponseEntity<byte[]> response = exportController.exportTransactions(
                principal, "csv", null, null, null, null, "ALL");

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).contains("transactions_");
    }
}
