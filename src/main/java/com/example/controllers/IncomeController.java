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
import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import java.security.Principal;
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
	public IncomeResponseDTO createIncome(Principal principal, @Valid @RequestBody IncomeRequestDTO requestDTO) {
		return incomeService.saveIncome(requestDTO, principal.getName());
	}
	
	@GetMapping
    public ResponseEntity<PagedResponse<IncomeResponseDTO>> getAllIncomes(Principal principal, @RequestParam(value= "page", defaultValue = "0", required = false) int page, @RequestParam(value = "size", defaultValue = "20", required = false) int size) {
    	PagedResponse<IncomeResponseDTO> response = incomeService.getIncomesForCurrentUserPaginated(principal.getName(), page, size);
        return ResponseEntity.ok(response);
    }
	
	@GetMapping("/{id}")
    public IncomeResponseDTO getIncomeById(Principal principal, @PathVariable int id) {
        return incomeService.getIncomeById(id, principal.getName());
    }

	@PutMapping("/{id}")
	public IncomeResponseDTO updateIncome(Principal principal, @PathVariable int id, @Valid @RequestBody IncomeRequestDTO requestDTO) {
    	return incomeService.updateIncome(id, requestDTO, principal.getName());
    }
	
	@PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncome(Principal principal, @PathVariable int id) {
    	incomeService.deleteIncome(id, principal.getName());
    }
	
	// Add filtering by income type
    @GetMapping("/by-type")
    public List<IncomeResponseDTO> getIncomesByType(
    		Principal principal, 
        @RequestParam String incomeType,
        @RequestParam(required = false) Integer userId
    ) {
        return incomeService.findByIncomeTypeName(incomeType, principal.getName());
    }
    
    @GetMapping("/total-month")
    public Double getTotalByMonth(Principal principal, @RequestParam String month) {
        return incomeService.getTotalIncomeAmountByMonth(month, principal.getName());
    }
	

}
