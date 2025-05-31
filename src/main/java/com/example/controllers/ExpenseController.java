package com.example.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dataTransferObjects.ExpenseRequestDTO;
import dataTransferObjects.ExpenseResponseDTO;
import jakarta.validation.Valid;
import service.ExpenseService;
import service.ExpenseSubtypeService;
import service.ExpenseTypeService;

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
    	return expenseService.saveExpense(null, null, null);
    }
    
 // Get all expenses
    @GetMapping
    public List<ExpenseResponseDTO> getAllExpenses(@RequestParam(required = false) Integer userId, @RequestParam(required = false) String month) {
    	return expenseService.getAllExpenses();
    }
    
}
