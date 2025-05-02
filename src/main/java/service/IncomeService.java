package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import models.Income;
import models.IncomeType;
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
	
	public List<Income> findByIncomeTypeName(IncomeType incomeType) {
		return incomeRepository.findByIncomeTypeName(incomeType.getTypeName());
	}
	
	public Double getTotalIncomeAmountByType(IncomeType incomeType) {
		return incomeRepository.getTotalIncomeAmountByType(incomeType.getTypeName());
	}
	
	public Double getTotalIncomeAmountByMonth(String month) {
		switch(month) {
    	case "January": 
    		return incomeRepository.getTotalIncomeAmountByMonth(1);
    	case "February": 
    		return incomeRepository.getTotalIncomeAmountByMonth(2);
    	case "March": 
    		return incomeRepository.getTotalIncomeAmountByMonth(3);
    	case "April": 
    		return incomeRepository.getTotalIncomeAmountByMonth(4);
    	case "Mai": 
    		return incomeRepository.getTotalIncomeAmountByMonth(5);
    	case "June": 
    		return incomeRepository.getTotalIncomeAmountByMonth(6);
    	case "July": 
    		return incomeRepository.getTotalIncomeAmountByMonth(7);
    	case "August": 
    		return incomeRepository.getTotalIncomeAmountByMonth(8);
    	case "September": 
    		return incomeRepository.getTotalIncomeAmountByMonth(8);
    	case "October": 
    		return incomeRepository.getTotalIncomeAmountByMonth(10);
    	case "November": 
    		return incomeRepository.getTotalIncomeAmountByMonth(11);
    	case "December": 
    		return incomeRepository.getTotalIncomeAmountByMonth(12);
    	default:
    		throw new IllegalArgumentException("Wrong month");
    	}
	}
	
	public double getTotalIncomeAmountByYear(Integer year) {
		return incomeRepository.getTotalIncomeAmountByYear(year);
	}
	

}
