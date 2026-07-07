package com.example.controllers;

import com.example.models.PaymentMethod;
import com.example.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactions(
            Principal principal,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "ALL") String kind) throws IOException {

        byte[] data = exportService.export(principal.getName(), format, startDate, endDate, categoryId, paymentMethod, kind);
        String extension = format.toLowerCase();
        String filename = "transactions_" + LocalDate.now() + "." + extension;

        MediaType mediaType = switch (extension) {
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.parseMediaType("text/csv");
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}