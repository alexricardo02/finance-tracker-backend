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
		if (expenseTypeRepository.existsByTypeName(expenseType.getTypeName())) {
            throw new IllegalArgumentException(
                "El tipo de gasto '" + expenseType.getTypeName() + "' ya existe"
            );
        }
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
    	if (!expenseTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Tipo de gasto no encontrado con ID: " + id);
        }
    	expenseTypeRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseType updateExpense(ExpenseType expenseType) {
    	
    	ExpenseType existingType = expenseTypeRepository.findById(expenseType.getTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de gasto no encontrado"));

            // Validar nombre único (si el nombre ha cambiado)
            if (!existingType.getTypeName().equals(expenseType.getTypeName()) 
                && expenseTypeRepository.existsByTypeName(expenseType.getTypeName())) {
                throw new IllegalArgumentException(
                    "El tipo de gasto '" + expenseType.getTypeName() + "' ya existe"
                );
            }
            
        existingType.setTypeName(expenseType.getTypeName());
    	
        return expenseTypeRepository.save(expenseType);
    }

    
    
    
    
}
