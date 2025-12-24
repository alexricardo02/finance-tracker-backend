package com.example.service;

import java.time.LocalDate;
import java.util.List;



import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
	
    // Método para crear un Expense con validación de tipo y subtipo
    @Transactional
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
    	
    	
    	// Build expense entity
        Expense expense = new Expense();
        expense.setExpenseAmount(requestDTO.getAmount());
        expense.setCurrency(requestDTO.getCurrency());
        expense.setExpenseDate(requestDTO.getDate());
        expense.setExpenseType(requestDTO.getTypeName()); // Set resolved subtype
        expense.setExpenseDescription(requestDTO.getDescription());
        
     // Resolve user
        User user = userRepository.findById(requestDTO.getUserId())
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
    
    public List<String> getAllExpenseTypes() {
    	return expenseRepository.findAllExpenseTypes();
    }

    public ExpenseResponseDTO getExpenseById(int id) { 
    	Expense expense = expenseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));
    	return convertToResponseDTO(expense);
    }

    @Transactional
    public void deleteExpense(int id) {
        expenseRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseResponseDTO  updateExpense(int expenseId, ExpenseRequestDTO requestDTO) {
    	
    	Expense existingExpense = expenseRepository.findById(expenseId).orElseThrow(() -> new IllegalArgumentException("Expense not found"));
    			
    	existingExpense.setExpenseAmount(requestDTO.getAmount());
        existingExpense.setExpenseDate(requestDTO.getDate());
        existingExpense.setExpenseType(requestDTO.getTypeName());
        existingExpense.setExpenseDescription(requestDTO.getDescription());
        Expense updatedExpense = expenseRepository.save(existingExpense);
        return convertToResponseDTO(updatedExpense);
    }

    public List<ExpenseResponseDTO> getExpenseByType(String expenseType) {

    	if (expenseType == null) {
            throw new IllegalArgumentException("ExpenseType no puede ser nulo");
        }
    	List<Expense> expenses = expenseRepository.findByExpenseTypeName(expenseType);
    	
    	return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    
    public Double getTotalExpenseAmountByType(String expenseType) {
    	return expenseRepository.getTotalExpenseAmountByType(expenseType);
    }
    
    public Double getTotalExpensesLast7DaysInclusive(LocalDate today) {
    	LocalDate start = today.minusDays(6);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalExpensesLastMonths(LocalDate today) {
    	LocalDate start = today.minusMonths(1);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalExpensesLast3Months(LocalDate today) {
    	LocalDate start = today.minusMonths(3);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalExpensesLast6Months(LocalDate today) {
    	LocalDate start = today.minusMonths(6);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalExpensesLastYear(LocalDate today) {
    	LocalDate start = today.minusYears(1);
    	LocalDate end = today;
    	Double result = expenseRepository.getTotalExpenseAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }    
    
    
    
    public Double getTotalExpenseAmounByDay(Integer day) {
    	switch(day) {
    	case 1: 
    		return expenseRepository.getTotalExpenseAmountByDay(1);
    	case 2: 
    		return expenseRepository.getTotalExpenseAmountByDay(2);
    	case 3: 
    		return expenseRepository.getTotalExpenseAmountByDay(3);
    	case 4: 
    		return expenseRepository.getTotalExpenseAmountByDay(4);
    	case 5: 
    		return expenseRepository.getTotalExpenseAmountByDay(5);
    	case 6: 
    		return expenseRepository.getTotalExpenseAmountByDay(6);
    	case 7: 
    		return expenseRepository.getTotalExpenseAmountByDay(7);
    	default:
    		throw new IllegalArgumentException("Wrong day");
    	}
    }
    
    
    public Double getTotalExpenseAmounByMonth(String month) {
    	switch(month) {
    	case "January": 
    		return expenseRepository.getTotalExpenseAmountByMonth(1);
    	case "February": 
    		return expenseRepository.getTotalExpenseAmountByMonth(2);
    	case "March": 
    		return expenseRepository.getTotalExpenseAmountByMonth(3);
    	case "April": 
    		return expenseRepository.getTotalExpenseAmountByMonth(4);
    	case "Mai": 
    		return expenseRepository.getTotalExpenseAmountByMonth(5);
    	case "June": 
    		return expenseRepository.getTotalExpenseAmountByMonth(6);
    	case "July": 
    		return expenseRepository.getTotalExpenseAmountByMonth(7);
    	case "August": 
    		return expenseRepository.getTotalExpenseAmountByMonth(8);
    	case "September": 
    		return expenseRepository.getTotalExpenseAmountByMonth(8);
    	case "October": 
    		return expenseRepository.getTotalExpenseAmountByMonth(10);
    	case "November": 
    		return expenseRepository.getTotalExpenseAmountByMonth(11);
    	case "December": 
    		return expenseRepository.getTotalExpenseAmountByMonth(12);
    	default:
    		throw new IllegalArgumentException("Wrong month");
    	}
    }
    
    public Double getTotalExpenseAmounByYear(Integer year) {
    	if (year == null) {
    		throw new IllegalArgumentException("El año no puede ser nulo");
    	}
    	return expenseRepository.getTotalExpenseAmountByYear(year);
    }
    
    
    
}
