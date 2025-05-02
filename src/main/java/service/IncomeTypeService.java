package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import models.IncomeType;
import repository.IncomeTypeRepository;

@Service
public class IncomeTypeService {
	
	@Autowired // 
    private IncomeTypeRepository incomeTypeRepository;
	
	// Método para guardar un gasto con validación
	@Transactional
    public IncomeType saveIncomeType(IncomeType incomeType) {
        return incomeTypeRepository.save(incomeType); // Usa el repositorio para guardar
    }
    
    public List<IncomeType> getAllIncomeTypes() {
        return incomeTypeRepository.findAll();
    }

    public IncomeType getIncomeTypeById(int id) {
        return incomeTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    @Transactional
    public void deleteIncomeType(int id) {
    	incomeTypeRepository.deleteById(id);
    }
    
    @Transactional
    public IncomeType updateIncome(IncomeType incomeType) {
        // Verifica que el tipo de gasto exista
        if (!incomeTypeRepository.existsById(incomeType.getTypeId())) {
            throw new RuntimeException("Tipo de gasto no existe");
        }
        return incomeTypeRepository.save(incomeType);
    }

}
