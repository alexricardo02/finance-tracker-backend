package com.example.controllers;

import java.util.List; 


import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
	
	private final ExpenseService expenseService;
    
    public ExpenseController(ExpenseService expenseService) {
    	this.expenseService = expenseService;
    }
    
    //Create an Expense
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDTO createExpense(@Valid @RequestBody ExpenseRequestDTO requestDTO) {
    	return expenseService.saveExpense(requestDTO); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }
    
 // Get all expenses
    @GetMapping
    public List<ExpenseResponseDTO> getAllExpenses(@RequestParam(required = false) Integer userId, @RequestParam(required = false) String month) {
    	return expenseService.getExpensesForCurrentUser();
    }
    
    @PutMapping("/{id}")
    public ExpenseResponseDTO updateExpense(@PathVariable int id, @Valid @RequestBody ExpenseRequestDTO requestDTO) {
    	return expenseService.updateExpense(id, requestDTO);
    	
    }
    
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable int id) {
    	expenseService.deleteExpense(id);
    }
    
    
    // implementar el resto de funciones
    
}
