package service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dataTransferObjects.ExpenseSubtypeResponseDTO;
import dataTransferObjects.ExpenseTypeRequestDTO;
import dataTransferObjects.ExpenseTypeResponseDTO;
import dataTransferObjects.IncomeTypeResponseDTO;
import jakarta.transaction.Transactional;
import models.ExpenseType;
import models.IncomeType;
import repository.ExpenseTypeRepository;

@Service
public class ExpenseTypeService {
	
	@Autowired // 
    private ExpenseTypeRepository expenseTypeRepository;

	
	
	// Helper method: Convert entity to DTO
    private ExpenseTypeResponseDTO convertToResponseDTO(ExpenseType expenseType) {
        ExpenseTypeResponseDTO dto = new ExpenseTypeResponseDTO();
        dto.setTypeId(expenseType.getTypeId());
        dto.setTypeName(expenseType.getTypeName());
        
        // Optional: Map subtypes if needed
        if (expenseType.getSubtypes() != null) {
            List<ExpenseSubtypeResponseDTO> subtypeDTOs = expenseType.getSubtypes().stream()
                .map(subtype -> new ExpenseSubtypeResponseDTO(
                    subtype.getSubtypeId(),
                    subtype.getSubtypeName(),
                    expenseType.getTypeName()
                ))
                .collect(Collectors.toList());
            dto.setSubtypes(subtypeDTOs);
        }
        return dto;
    }
	
	// Método para guardar un gasto con validación
	@Transactional
    public ExpenseTypeResponseDTO saveExpenseType(ExpenseTypeRequestDTO  expenseTypeDTO) {
		
		if (expenseTypeRepository.existsByTypeName(expenseTypeDTO.getTypeName())) {
            throw new IllegalArgumentException(
                "El tipo de gasto '" + expenseTypeDTO.getTypeName() + "' ya existe"
            );
        }
		
		// Convert DTO to entity
        ExpenseType expenseType = new ExpenseType();
        expenseType.setTypeName(expenseTypeDTO.getTypeName());

        // Save and convert to DTO
        ExpenseType savedType = expenseTypeRepository.save(expenseType);
        return convertToResponseDTO(savedType);
    }
    
    public List<ExpenseTypeResponseDTO> getAllExpenseTypes() {
    	List<ExpenseType> expenseTypes = expenseTypeRepository.findAll();
    	
    	return expenseTypes.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    public ExpenseTypeResponseDTO getExpenseTypeById(int id) {
    	ExpenseType expenseType = expenseTypeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Expensesubtype not found with ID: " + id));

    	return convertToResponseDTO(expenseType);
    }

    @Transactional
    public void deleteExpenseType(int id) {
    	if (!expenseTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Tipo de gasto no encontrado con ID: " + id);
        }
    	expenseTypeRepository.deleteById(id);
    }
    
    @Transactional
    public ExpenseTypeResponseDTO updateExpense(int typeId, ExpenseTypeRequestDTO requestDTO) {
        ExpenseType existingType = expenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de gasto no encontrado"));

            // Validate uniqueness if name changes
            if (!existingType.getTypeName().equals(requestDTO.getTypeName()) 
                && expenseTypeRepository.existsByTypeName(requestDTO.getTypeName())) {
                throw new IllegalArgumentException(
                    "El tipo de gasto '" + requestDTO.getTypeName() + "' ya existe"
                );
            }

            // Update fields
            existingType.setTypeName(requestDTO.getTypeName());

            // Save and convert to DTO
            ExpenseType updatedType = expenseTypeRepository.save(existingType);
            return convertToResponseDTO(updatedType);
        }
}
