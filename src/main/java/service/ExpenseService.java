package service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dataTransferObjects.ExpenseRequestDTO;
import dataTransferObjects.ExpenseResponseDTO;
import jakarta.transaction.Transactional;
import models.Expense;
import models.ExpenseSubtype;
import models.ExpenseType;
import repository.ExpenseRepository;
import repository.ExpenseSubtypeRepository;
import repository.ExpenseTypeRepository;

@Service 
public class ExpenseService {
	
	@Autowired // 
    private ExpenseRepository expenseRepository;
	
	@Autowired
    private ExpenseSubtypeRepository expenseSubtypeRepository;

    @Autowired
    private ExpenseTypeRepository expenseTypeRepository;
    
    @Autowired
    private ExpenseSubtypeService expenseSubtypeService; // Injected dependency
    
    // Helper method to convert Expense → ExpenseResponseDTO
    private ExpenseResponseDTO convertToResponseDTO(Expense expense) {
        return new ExpenseResponseDTO(
            expense.getExpenseID(),
            expense.getExpenseAmount(),
            expense.getExpenseDate(),
            expense.getExpenseSubtype().getSubtypeName(),
            expense.getExpenseSubtype().getType().getTypeName(),
            expense.getUser().getUserId()
        );
    }
	
    // Método para crear un Expense con validación de tipo y subtipo
    @Transactional
    public ExpenseResponseDTO saveExpense(Expense expense, String typeName, String subtypeName) {

    	
    	if (expense == null || typeName == null || subtypeName == null) {
            throw new IllegalArgumentException("Parámetros no pueden ser nulos");
        }
    	// 1. Buscar el ExpenseType por nombre
    	ExpenseType expenseType = expenseTypeRepository.findByTypeName(typeName);
    	if (expenseType == null) {
            throw new RuntimeException("Tipo no encontrado: " + typeName);
        }
    	
    	// 2. Buscar el ExpenseSubtype por nombre y tipo
    	Optional<ExpenseSubtype> subtype = expenseSubtypeRepository.findBySubtypeNameAndExpenseType(subtypeName, expenseType);
    	if (!subtype.isPresent()) { // Verificar si el Optional está vacío
            throw new RuntimeException(
                "Subtipo '" + subtypeName + "' no pertenece al tipo '" + typeName + "'"
            );
        }
    	
    	expense.setExpenseSubtype(subtype.get());
    	Expense expenseToSave = expenseRepository.save(expense);
    	return convertToResponseDTO(expenseToSave);
    	
    }
    
   
    public List<ExpenseResponseDTO> getAllExpenses() { 
    	List<Expense> expenses = expenseRepository.findAll();
    	return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
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
        
        ExpenseSubtype subtype = expenseSubtypeService.findBySubtypeName(requestDTO.getSubtypeName());
        
        existingExpense.setExpenseSubtype(subtype);
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
    
    public List<ExpenseResponseDTO> getExpenseBySubtype(String expenseSubtype) {

    	List<Expense> expenses = expenseRepository.findByExpenseSubtypeName(expenseSubtype);
    	if (expenseSubtype == null) {
            throw new IllegalArgumentException("ExpenseType no puede ser nulo");
        }
    	return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    public Double getTotalExpenseAmountByType(ExpenseType expenseType) {
    	return expenseRepository.getTotalExpenseAmountByType(expenseType.getTypeName());
    }
    
    public Double getTotalExpenseAmountBySubtype(ExpenseSubtype expenseSubtype) {
    	return expenseRepository.getTotalExpenseAmountBySubtype(expenseSubtype.getSubtypeName());
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
    	return expenseRepository.getTotalExpenseAmountByYear(year);
    }
    
}
