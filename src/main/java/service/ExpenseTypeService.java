package service;

import java.util.List; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import models.ExpenseType;
import repository.ExpenseTypeRepository;

@Service
public class ExpenseTypeService {
	
	@Autowired // 
    private ExpenseTypeRepository expenseTypeRepository;
	
	// Método para guardar un gasto con validación
    public ExpenseType saveExpenseType(ExpenseType expenseType) {
        return expenseTypeRepository.save(expenseType); // Usa el repositorio para guardar
    }
    
    public List<ExpenseType> getAllExpenseTypes() {
        return expenseTypeRepository.findAll();
    }

    public ExpenseType getExpenseTypeById(int id) {
        return expenseTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    public void deleteExpenseType(int id) {
    	expenseTypeRepository.deleteById(id);
    }
    
    public ExpenseType updateExpense(ExpenseType expenseType) {
        // Verifica que el tipo de gasto exista
        if (!expenseTypeRepository.existsById(expenseType.getTypeId())) {
            throw new RuntimeException("Tipo de gasto no existe");
        }
        return expenseTypeRepository.save(expenseType);
    }

}
