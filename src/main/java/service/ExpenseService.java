package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import models.Expense;
import models.ExpenseSubtype;
import models.ExpenseType;
import repository.ExpenseRepository;

@Service 
public class ExpenseService {
	
	@Autowired // 
    private ExpenseRepository expenseRepository;
	
	// Método para guardar un gasto con validación
    public Expense saveExpense(Expense expense) {
        // Validación: El monto no puede ser negativo
        if (expense.getExpenseAmount() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        return expenseRepository.save(expense); // Usa el repositorio para guardar
    }
    
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense getExpenseById(int id) {
        return expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    public void deleteExpense(int id) {
        expenseRepository.deleteById(id);
    }
    
    public Expense updateExpense(Expense expense) {
        // Verifica que el gasto exista
        if (!expenseRepository.existsById(expense.getExpenseID())) {
            throw new RuntimeException("Gasto no existe");
        }
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getExpenseByType(ExpenseType expenseType) {
    	return expenseRepository.findByExpenseTypeName(expenseType.getTypeName());
    }
    
    
    public List<Expense> getExpenseBySubtype(ExpenseSubtype expenseSubtype) {
    	return expenseRepository.findByExpenseSubtypeName(expenseSubtype.getSubtypeName());
    }
    
    public Double getTotalExpenseAmountByType(ExpenseType expenseType) {
    	return expenseRepository.getTotalExpenseAmountByType(expenseType.getTypeName());
    }
    
    public Double getTotalExpenseAmountBySubtype(ExpenseSubtype expenseSubtype) {
    	return expenseRepository.getTotalExpenseAmountBySubtype(expenseSubtype.getSubtypeName());
    }
    
    
    
    
   
    
    
    
    
    

}
