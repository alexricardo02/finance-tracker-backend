package com.example.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import jakarta.validation.Valid;
import com.example.service.IncomeService;


@RestController
@RequestMapping("/api/incomes")
public class IncomeController {
	
	private final IncomeService incomeService;

	public IncomeController(IncomeService incomeService) {
		super();
		this.incomeService = incomeService;
	}
	
	@PostMapping
    @ResponseStatus(HttpStatus.CREATED)
	public IncomeResponseDTO createIncome(@Valid @RequestBody IncomeRequestDTO requestDTO) {
		return incomeService.saveIncome(requestDTO);
	}
	
	@GetMapping
	public List<IncomeResponseDTO> getAllIncomes(@RequestParam(required = false) Integer userId, @RequestParam(required = false) String month) {
		return incomeService.getIncomesForCurrentUser();
	}
	
	@GetMapping("/{id}")
    public IncomeResponseDTO getIncomeById(@PathVariable int id) {
        return incomeService.getIncomeById(id);
    }

	@PutMapping("/{id}")
	public IncomeResponseDTO updateIncome(@PathVariable int id, @Valid @RequestBody IncomeRequestDTO requestDTO) {
    	return incomeService.updateIncome(id, requestDTO);
    }
	
	@PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable int id) {
    	incomeService.deleteIncome(id);
    }
	
	// Add filtering by income type
    @GetMapping("/by-type")
    public List<IncomeResponseDTO> getIncomesByType(
        @RequestParam String incomeType,
        @RequestParam(required = false) Integer userId
    ) {
    	String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return incomeService.findByIncomeTypeName(incomeType, username);
    }
    
    @GetMapping("/total-month")
    public Double getTotalByMonth(@RequestParam String month) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return incomeService.getTotalIncomeAmountByMonth(month, username);
    }
	

}
