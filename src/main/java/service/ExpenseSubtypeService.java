package service;

import java.util.List; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import models.ExpenseSubtype;
import repository.ExpenseSubtypeRepository;

@Service
public class ExpenseSubtypeService {
	
	@Autowired // 
    private ExpenseSubtypeRepository expenseSubtypeRepository;
	
	// Método para guardar un gasto con validación
	@Transactional
    public ExpenseSubtype saveExpenseSubtype(ExpenseSubtype expenseSubtype) {
        return expenseSubtypeRepository.save(expenseSubtype); // Usa el repositorio para guardar
    }
    
    public List<ExpenseSubtype> getAllExpenseSubtypes() {
        return expenseSubtypeRepository.findAll();
    }

    public ExpenseSubtype getExpenseSubtypeById(int id) {
        return expenseSubtypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    @Transactional
    public void deleteExpenseSubtype(int id) {
    	expenseSubtypeRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseSubtype updateExpenseSubtype(ExpenseSubtype expenseSubtype) {
        return expenseSubtypeRepository.save(expenseSubtype);
    }

}
