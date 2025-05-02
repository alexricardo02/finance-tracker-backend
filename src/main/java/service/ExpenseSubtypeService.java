package service;

import java.util.List; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import models.ExpenseSubtype;
import repository.ExpenseSubtypeRepository;

@Service
public class ExpenseSubtypeService {
	
	@Autowired // 
    private ExpenseSubtypeRepository expenseSubtypeRepository;
	
	// Método para guardar un gasto con validación
    public ExpenseSubtype saveExpenseType(ExpenseSubtype expenseSubtype) {
        return expenseSubtypeRepository.save(expenseSubtype); // Usa el repositorio para guardar
    }
    
    public List<ExpenseSubtype> getAllExpenseTypes() {
        return expenseSubtypeRepository.findAll();
    }

    public ExpenseSubtype getExpenseTypeById(int id) {
        return expenseSubtypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    public void deleteExpenseType(int id) {
    	expenseSubtypeRepository.deleteById(id);
    }
    
    public ExpenseSubtype updateExpense(ExpenseSubtype expenseSubtype) {
        // Verifica que el tipo de gasto exista
        if (!expenseSubtypeRepository.existsById(expenseSubtype.getSubtypeId())) {
            throw new RuntimeException("Tipo de gasto no existe");
        }
        return expenseSubtypeRepository.save(expenseSubtype);
    }

}
