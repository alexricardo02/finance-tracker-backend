package com.example.controllers;

import com.example.dataTransferObjects.ImportResultDTO;
import com.example.dataTransferObjects.TransactionImportRowDTO;
import com.example.service.ImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/imports")
@Validated
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResultDTO importTransactions(
            Principal principal,
            @Valid @RequestBody @Size(min = 1, max = 2000, message = "Between 1 and 2000 rows allowed")
            List<@Valid TransactionImportRowDTO> rows) {
        return importService.importTransactions(rows, principal.getName());
    }
}