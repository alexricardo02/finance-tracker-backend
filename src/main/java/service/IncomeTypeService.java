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
		if (incomeType.getTypeName() == null || incomeType.getTypeName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tipo de ingreso no puede estar vacío");
        }
		if (incomeTypeRepository.incomeTypeExistsById(incomeType.getTypeId())) {
            throw new IllegalArgumentException("El tipo de ingreso '" + incomeType.getTypeName() + "' ya existe");
        }

        return incomeTypeRepository.save(incomeType); // Usa el repositorio para guardar
    }
    
    public List<IncomeType> getAllIncomeTypes() {
        return incomeTypeRepository.findAll();
    }
                             
    
    public IncomeType getIncomeTypeById(Integer id) {
        return incomeTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));
    }

    
    @Transactional
    public void deleteIncomeType(Integer id) {
    	IncomeType incomeType = incomeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ingreso no encontrado con ID: " + id));
        incomeTypeRepository.delete(incomeType);
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
