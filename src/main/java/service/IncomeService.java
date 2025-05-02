package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import models.Income;
import repository.IncomeRepository;

@Service
public class IncomeService {
	
	@Autowired // 
    private IncomeRepository incomeRepository;
	
	public Income saveIncome(Income income) {
        // Validación: El monto no puede ser negativo
        if (income.getIncomeAmount() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        return incomeRepository.save(income); // Usa el repositorio para guardar
    }
	
	public List<Income> getAllIncomes() {
		return incomeRepository.findAll();
	};
	
	public Income getIncomeById(int id) {
		return incomeRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));
	}
	
	public void deleteIncome(int id) {
		incomeRepository.deleteById(id);
	}
	
	public Income updateIncome(Income income) {
		// Verifica que el ingreso exista
        if (!incomeRepository.existsById(income.getIncomeId())) {
            throw new RuntimeException("Gasto no existe");
        }
        return incomeRepository.save(income);
	}
	
	
	

}
