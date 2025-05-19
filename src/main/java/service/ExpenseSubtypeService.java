package service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dataTransferObjects.ExpenseRequestDTO;
import dataTransferObjects.ExpenseSubtypeRequestDTO;
import dataTransferObjects.ExpenseSubtypeResponseDTO;
import jakarta.transaction.Transactional;
import models.Expense;
import models.ExpenseSubtype;
import models.ExpenseType;
import repository.ExpenseSubtypeRepository;
import repository.ExpenseTypeRepository;

@Service
public class ExpenseSubtypeService {
	
	@Autowired // 
    private ExpenseSubtypeRepository expenseSubtypeRepository;
	@Autowired
    private ExpenseTypeRepository expenseTypeRepository;

	
	private ExpenseSubtypeResponseDTO convertToResponseDTO(ExpenseSubtype expenseSubtype) {
        return new ExpenseSubtypeResponseDTO(
        	expenseSubtype.getSubtypeId(),
        	expenseSubtype.getSubtypeName(),
        	expenseSubtype.getType().toString()
        );
    }
	
	// Método para guardar un gasto con validación
	@Transactional
    public ExpenseSubtypeResponseDTO saveExpenseSubtype(ExpenseSubtype expenseSubtype) {
		
		if (expenseSubtype == null) {
            throw new IllegalArgumentException("Parámetros no pueden ser nulos");
        }
		
    	ExpenseSubtype expenseSubtypeToSave = expenseSubtypeRepository.save(expenseSubtype);
		
        return convertToResponseDTO(expenseSubtypeToSave); // Usa el repositorio para guardar
    }
    
    public List<ExpenseSubtypeResponseDTO> getAllExpenseSubtypes() {
    	List<ExpenseSubtype> expenseSubtypes = expenseSubtypeRepository.findAll();
    	
    	return expenseSubtypes.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    public ExpenseSubtypeResponseDTO getExpenseSubtypeById(int id) {
    	ExpenseSubtype expenseSubtype = expenseSubtypeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Expensesubtype not found with ID: " + id));
        return convertToResponseDTO(expenseSubtype);
    }

    @Transactional
    public void deleteExpenseSubtype(int id) {
    	expenseSubtypeRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseSubtypeResponseDTO updateExpenseSubtype(int expenseSubtypeId, ExpenseSubtypeRequestDTO requestDTO) {
    	ExpenseSubtype existingExpenseSubtype = expenseSubtypeRepository.findById(expenseSubtypeId).orElseThrow(() -> new IllegalArgumentException("Expense subtype not found"));
		
    	if (requestDTO.getTypeId() != null) {
    	    ExpenseType newType = expenseTypeRepository.findById(requestDTO.getTypeId())
    	        .orElseThrow(() -> new IllegalArgumentException("ExpenseType not found"));
    	    existingExpenseSubtype.setType(newType);
    	}
    	
    	existingExpenseSubtype.setSubtypeName(requestDTO.getSubtypeName());

        expenseSubtypeRepository.save(existingExpenseSubtype);
        return convertToResponseDTO(existingExpenseSubtype);
    }
    
    // For internal operations
    public ExpenseSubtype findBySubtypeName(String subtypeName) {
    	
    	Optional<ExpenseSubtype> optionalSubtype = expenseSubtypeRepository.findBySubtypeName(subtypeName);
    	
    	return optionalSubtype.orElseThrow(()-> new IllegalArgumentException("Subtype not found: " + subtypeName));
    } 
    
    // For API
    public ExpenseSubtypeResponseDTO getSubtypeByName(String subtypeName) {
        ExpenseSubtype subtype = findBySubtypeName(subtypeName); // Reuse internal method
        return convertToResponseDTO(subtype);
    }

}
