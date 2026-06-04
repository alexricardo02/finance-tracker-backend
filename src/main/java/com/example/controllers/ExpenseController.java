package com.example.controllers;

import java.util.List; 




import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.dataTransferObjects.*;
import jakarta.validation.Valid;
import com.example.service.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
	
	private final ExpenseService expenseService;
    
    public ExpenseController(ExpenseService expenseService) {
    	this.expenseService = expenseService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDTO createExpense(Principal principal, @Valid @RequestBody ExpenseRequestDTO requestDTO) {
    	return expenseService.saveExpense(requestDTO, principal.getName()); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<ExpenseResponseDTO>> getAllExpenses(Principal principal, @RequestParam(value= "page", defaultValue = "0", required = false) int page, @RequestParam(value = "size", defaultValue = "20", required = false) int size) {
    	PagedResponse<ExpenseResponseDTO> response = expenseService.getExpensesForCurrentUserPaginated(principal.getName(), page, size);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ExpenseResponseDTO updateExpense(Principal principal, @PathVariable int id, @Valid @RequestBody ExpenseRequestDTO requestDTO) {
    	return expenseService.updateExpense(id, requestDTO, principal.getName());
    	
    }
    
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(Principal principal, @PathVariable int id) {
    	expenseService.deleteExpense(id, principal.getName());
    }
    
    
    
}
