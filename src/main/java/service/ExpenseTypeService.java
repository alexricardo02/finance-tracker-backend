package service;

import java.util.List; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import models.ExpenseType;
import repository.ExpenseTypeRepository;

@Service
public class ExpenseTypeService {
	
	@Autowired // 
    private ExpenseTypeRepository expenseTypeRepository;
	
	// Método para guardar un gasto con validación
	@Transactional
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

    @Transactional
    public void deleteExpenseType(int id) {
    	expenseTypeRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseType updateExpense(ExpenseType expenseType) {
        // Verifica que el tipo de gasto exista
        if (!expenseTypeRepository.existsById(expenseType.getTypeId())) {
            throw new RuntimeException("Tipo de gasto no existe");
        }
        return expenseTypeRepository.save(expenseType);
    }

}
