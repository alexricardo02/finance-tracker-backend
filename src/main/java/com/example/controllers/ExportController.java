package com.example.controllers;

import com.example.models.PaymentMethod;
import com.example.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    // H-4 fix: allowlist of accepted format values.
    // The format value goes directly into the Content-Disposition filename and
    // into a switch-expression used to resolve the MediaType.
    // Without validation, a crafted value like "csv%0d%0aX-Evil: hdr" would
    // inject arbitrary HTTP response headers (CRLF injection).
    private static final Set<String> ALLOWED_FORMATS = Set.of("csv", "xlsx", "pdf");

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions")
    public ResponseEntity<byte[]> exportTransactions(
            Principal principal,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "ALL") String kind) throws IOException {

        // H-4 fix: reject any format not on the allowlist before it touches a header
        String safeFormat = format.toLowerCase();
        if (!ALLOWED_FORMATS.contains(safeFormat)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unsupported export format '" + safeFormat + "'. Allowed values: csv, xlsx, pdf");
        }

        byte[] data = exportService.export(principal.getName(), safeFormat, startDate, endDate, categoryId, paymentMethod, kind);

        // safeFormat is now guaranteed to be one of {"csv","xlsx","pdf"} — no injection possible
        String filename = "transactions_" + LocalDate.now() + "." + safeFormat;

        MediaType mediaType = switch (safeFormat) {
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "pdf"  -> MediaType.APPLICATION_PDF;
            default     -> MediaType.parseMediaType("text/csv");
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}