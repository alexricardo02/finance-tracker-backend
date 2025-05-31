package service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dataTransferObjects.IncomeTypeRequestDTO;
import dataTransferObjects.IncomeTypeResponseDTO;
import jakarta.transaction.Transactional;
import models.IncomeType;
import repository.IncomeTypeRepository;

@Service
public class IncomeTypeService {
	
	@Autowired // 
    private IncomeTypeRepository incomeTypeRepository;
	
	// Convert entity → DTO
    private IncomeTypeResponseDTO convertToResponseDTO(IncomeType incomeType) {
        IncomeTypeResponseDTO dto = new IncomeTypeResponseDTO();
        dto.setTypeId(incomeType.getTypeId());
        dto.setTypeName(incomeType.getTypeName());
        return dto;
    }
	
	// Método para guardar un gasto con validación
	@Transactional
    public IncomeTypeResponseDTO saveIncomeType(IncomeTypeRequestDTO incomeType) {
		if (incomeType.getTypeName() == null || incomeType.getTypeName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tipo de ingreso no puede estar vacío");
        }
		if (incomeTypeRepository.existsByTypeName(incomeType.getTypeName())) {
            throw new IllegalArgumentException(
                "El tipo de gasto '" + incomeType.getTypeName() + "' ya existe"
            );
        }
		
		IncomeType incomeTypeResult = new IncomeType();
		incomeTypeResult.setTypeName(incomeType.getTypeName());

        // Save and convert to DTO
        IncomeType savedType = incomeTypeRepository.save(incomeTypeResult);
        return convertToResponseDTO(savedType);

    }
    
    public List<IncomeTypeResponseDTO> getAllIncomeTypes() {
    	List<IncomeType> incomeTypes = incomeTypeRepository.findAll();
    	return incomeTypes.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }
    
    
    public IncomeTypeResponseDTO getIncomeTypeById(Integer id) {
        IncomeType incomeType = incomeTypeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Incomesubtype not found with ID: " + id));

    	return convertToResponseDTO(incomeType);
    }

    
    @Transactional
    public void deleteIncomeType(Integer id) {
    	IncomeType incomeType = incomeTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ingreso no encontrado con ID: " + id));
        incomeTypeRepository.delete(incomeType);
    }
    
    
    @Transactional
    public IncomeTypeResponseDTO updateIncomeType(int typeId, IncomeTypeRequestDTO requestDTO) {
    	IncomeType existingType = incomeTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ingreso no encontrado"));
    	
    	// Validate uniqueness if name changes
        if (!existingType.getTypeName().equals(requestDTO.getTypeName()) 
            && incomeTypeRepository.existsByTypeName(requestDTO.getTypeName())) {
            throw new IllegalArgumentException(
                "El tipo de ingreso '" + requestDTO.getTypeName() + "' ya existe"
            );
        }
    	
        // Update fields
        existingType.setTypeName(requestDTO.getTypeName());

        // Save and convert to DTO
        IncomeType updatedType = incomeTypeRepository.save(existingType);
        return convertToResponseDTO(updatedType);
    }    

}
