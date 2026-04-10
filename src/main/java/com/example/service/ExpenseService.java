package com.example.service;

import java.time.LocalDate;
import java.util.List;



import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.dataTransferObjects.ExpenseRequestDTO;
import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.models.Expense;
import com.example.models.User;
import com.example.repository.ExpenseRepository;
import com.example.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service 
public class ExpenseService {
	
	@Autowired
    private ExpenseRepository expenseRepository;
	
	@Autowired UserRepository userRepository;
    
    // Helper method to convert Expense → ExpenseResponseDTO
    private ExpenseResponseDTO convertToResponseDTO(Expense expense) {
        return new ExpenseResponseDTO(
            expense.getExpenseID(),
            expense.getExpenseAmount(),
            expense.getCurrency(),
            expense.getExpenseDate(),
            expense.getExpenseType(),
            expense.getExpenseDescription(),
            expense.getUser().getUserId()
        );
    }
    
    private Integer getUserIdByUsername(String username) {
	    return userRepository.findByUsername(username)
	            .orElseThrow(() -> new RuntimeException("User not found"))
	            .getUserId();
	}
	
	
    public List<ExpenseResponseDTO> getExpensesForCurrentUser() {
        // 1. Le preguntamos a Spring Security: "¿Quién está logueado?"
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Buscamos a ese usuario en la DB
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Usamos la nueva función del repositorio
        List<Expense> expenses = expenseRepository.findByUserUserId(user.getUserId());

        // 4. Mapeamos a DTO (esto ya lo sabías hacer)
        return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    // Método para crear un Expense con validación de tipo y subtipo
    @Transactional
	@CacheEvict(value = {"all_expenses_types", "expenses_type", "expenses_last_7_days", "expenses_last_months", "expenses_last_3_months", "expenses_last_6_months", "expenses_last_year", "expenses_day", "expenses_month", "expenses_year"}, allEntries = true)	
    public ExpenseResponseDTO saveExpense(ExpenseRequestDTO requestDTO) {
    	
    	if (requestDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        if (requestDTO.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
    	
    	if (requestDTO.getTypeName() == null) {
            // Si el servicio devuelve Optional, usa orElseThrow ahí
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "ExpenseSubtype not found for typeName=" + requestDTO.getTypeName());
        }
    	
    	String authName = SecurityContextHolder.getContext().getAuthentication().getName();
    	
    	
    	// Build expense entity
        Expense expense = new Expense();
        expense.setExpenseAmount(requestDTO.getAmount());
        expense.setCurrency(requestDTO.getCurrency());
        expense.setExpenseDate(requestDTO.getDate());
        expense.setExpenseType(requestDTO.getTypeName()); // Set resolved subtype
        expense.setExpenseDescription(requestDTO.getDescription());
        
        
     // Resolve user
        User user = userRepository.findByUsername(authName)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        expense.setUser(user);
        
        
     // Save and convert to DTO
        Expense savedExpense = expenseRepository.save(expense);
        return convertToResponseDTO(savedExpense);
    }
    
   
    public List<ExpenseResponseDTO> getAllExpenses() { 
    	List<Expense> expenses = expenseRepository.findAll();
    	return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    
    @Cacheable(value = "all_expenses_types", key = "'all'")
    public List<String> getAllExpenseTypes() {
    	return expenseRepository.findAllExpenseTypes();
    }

    public ExpenseResponseDTO getExpenseById(int id) { 
    	Expense expense = expenseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));
    	return convertToResponseDTO(expense);
    }

    @Transactional
	@CacheEvict(value = {"all_expenses_types", "expenses_type", "expenses_last_7_days", "expenses_last_months", "expenses_last_3_months", "expenses_last_6_months", "expenses_last_year", "expenses_day", "expenses_month", "expenses_year"}, allEntries = true)	
    public void deleteExpense(int id) {
        expenseRepository.deleteById(id);
    }
    
    @Transactional
	@CacheEvict(value = {"all_expenses_types", "expenses_type", "expenses_last_7_days", "expenses_last_months", "expenses_last_3_months", "expenses_last_6_months", "expenses_last_year", "expenses_day", "expenses_month", "expenses_year"}, allEntries = true)	
    public ExpenseResponseDTO  updateExpense(int expenseId, ExpenseRequestDTO requestDTO) {
    	
    	Expense existingExpense = expenseRepository.findById(expenseId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));
    			
    	existingExpense.setExpenseAmount(requestDTO.getAmount());
        existingExpense.setExpenseDate(requestDTO.getDate());
        existingExpense.setExpenseType(requestDTO.getTypeName());
        existingExpense.setExpenseDescription(requestDTO.getDescription());
        Expense updatedExpense = expenseRepository.save(existingExpense);
        return convertToResponseDTO(updatedExpense);
    }

    public List<ExpenseResponseDTO> getExpenseByTypeAndUser(String expenseType, String username) {

    	if (expenseType == null) {
            throw new IllegalArgumentException("ExpenseType no puede ser nulo");
        }
    	List<Expense> expenses = expenseRepository.findByExpenseTypeNameAndUser(expenseType, getUserIdByUsername(username));
    	
    	return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    @Cacheable(value = "expenses_type", key = "#username + '_'+ #expenseType")
    public Double getTotalExpenseAmountByTypeAndUser(String expenseType, String username) {
    	return expenseRepository.getTotalExpenseAmountByTypeAndUser(expenseType, getUserIdByUsername(username));
    }
    
    @Cacheable(value= "expenses_last_7_days", key="#username + '_' + #today")
    public Double getTotalExpensesLast7DaysInclusiveAndUser(LocalDate today, String username) {
    	LocalDate start = today.minusDays(6);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value= "expenses_last_months", key="#username + '_' + #today")
    public Double getTotalExpensesLastMonthsAndUser(LocalDate today, String username) {
    	LocalDate start = today.minusMonths(1);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value= "expenses_last_3_months", key="#username + '_' + #today")
    public Double getTotalExpensesLast3MonthsAndUser(LocalDate today, String username) {
    	LocalDate start = today.minusMonths(3);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value= "expenses_last_6_months", key="#username + '_' + #today")
    public Double getTotalExpensesLast6MonthsAndUser(LocalDate today, String username) {
    	LocalDate start = today.minusMonths(6);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value= "expenses_last_year", key="#username + '_' + #today")
    public Double getTotalExpensesLastYearAndUser(LocalDate today, String username) {
    	LocalDate start = today.minusYears(1);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }    
    
    //Redis
    @Cacheable(value = "expenses_day", key = "#username + '_'+ #day")
    public Double getTotalExpenseAmounByDayAndUser(Integer day, String username) {
    	
    	if (day == null || day < 1 || day > 31) {
	        throw new IllegalArgumentException("Invalid day");
	    }
    	
    	return expenseRepository.getTotalExpenseAmountByDayAndUser(day, getUserIdByUsername(username));
  
    }
    
    //Redis
    @Cacheable(value= "expenses_month", key="#username + '_' + #month")
    public Double getTotalExpenseAmounByMonthAndUser(String month, String username) {
    	
    	try {
    		int monthNumber = java.time.Month.valueOf(month.toUpperCase()).getValue();
    		return expenseRepository.getTotalExpenseAmountByMonthAndUser(monthNumber, getUserIdByUsername(username));
    	} catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid month: " + month);
    	}
    }
    
    // Redis
    @Cacheable(value = "expenses_year", key = "#username + '_' + #year")
    public Double getTotalExpenseAmounByYearAndUser(Integer year, String username) {
    	if (year == null) {
    		throw new IllegalArgumentException("El año no puede ser nulo");
    	}
    	return expenseRepository.getTotalExpenseAmountByYearAndUser(year, getUserIdByUsername(username));
    }
    
    
    
}
