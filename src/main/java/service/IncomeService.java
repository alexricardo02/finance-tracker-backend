package service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dataTransferObjects.IncomeRequestDTO;
import dataTransferObjects.IncomeResponseDTO;
import jakarta.transaction.Transactional;
import models.Income;
import models.IncomeType;
import models.User;
import repository.IncomeRepository;
import repository.IncomeTypeRepository;
import repository.UserRepository;

@Service
public class IncomeService {
	
	@Autowired 
    private IncomeRepository incomeRepository;
	
	@Autowired
    private IncomeTypeRepository incomeTypeRepository;

    @Autowired
    private UserRepository userRepository;
	
	@Transactional
	public IncomeResponseDTO saveIncome(IncomeRequestDTO requestDTO) {
		
		// 1. Convertir DTO a entidad
        Income income = new Income();
        income.setIncomeAmount(requestDTO.getAmount());
        income.setIncomeDate(requestDTO.getIncomeDate());
        income.setIncomeDescription(requestDTO.getIncomeDescription());
        
        // 2. Buscar relaciones usando repositorios (no se confía en el cliente)
        IncomeType type = incomeTypeRepository.findByTypeName(requestDTO.getIncomeTypeName())
        		.orElseThrow(() -> new RuntimeException("Tipo no encontrado"));
        
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        income.setIncomeType(type);
        income.setUser(user);
        
        // 3. Guardar en la base de datos
        Income savedIncome = incomeRepository.save(income);
        
        // 4. Convertir entidad a ResponseDTO
        return convertToResponseDTO(savedIncome);
    }

	
	public List<Income> getAllIncomes() {
		return incomeRepository.findAll();
	};
	
	public Income getIncomeById(Integer id) {
		return incomeRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));
	}
	
	@Transactional
	public void deleteIncome(Integer id) {
		if (!incomeRepository.existsById(id)) {
            throw new RuntimeException("Ingreso no encontrado con ID: " + id);
        }
		incomeRepository.deleteById(id);
	}
	
	@Transactional
	public IncomeResponseDTO  updateIncome(Integer incomeId, IncomeRequestDTO incomeRequestDTO) {
		
		// 1. Buscar el ingreso existente
		Income existingIncome = incomeRepository.findById(incomeId)
				.orElseThrow(() -> new RuntimeException("Ingreso no encontrado con ID: " + incomeId));
		
		// 2. Actualizar campos básicos
		existingIncome.setIncomeAmount(incomeRequestDTO.getAmount());
		existingIncome.setIncomeDate(incomeRequestDTO.getIncomeDate());
		existingIncome.setIncomeDescription(incomeRequestDTO.getIncomeDescription());
		
		// 3. Actualizar relaciones (si se proporcionan en el DTO)
		// Esta funcion corrobora que el nuevo type no sea null
		if (incomeRequestDTO.getIncomeTypeName() != null) {
			IncomeType newType = incomeTypeRepository.findByTypeName(incomeRequestDTO.getIncomeTypeName())
					.orElseThrow(() -> new RuntimeException("Tipo de ingreso no encontrado: " + incomeRequestDTO.getIncomeTypeName()));
            existingIncome.setIncomeType(newType);
		}
		
		if (incomeRequestDTO.getUserId() != null) {
			User newUser = userRepository.findById(incomeRequestDTO.getUserId())
					.orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + incomeRequestDTO.getUserId()));
			existingIncome.setUser(newUser);
		}
		
		// 4. Guardar cambios
        Income updatedIncome = incomeRepository.save(existingIncome);
        
        // 5. Convertir a DTO de respuesta
        return convertToResponseDTO(updatedIncome);
		
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
	
	
	
	
	
	private IncomeResponseDTO convertToResponseDTO(Income income) {
        IncomeResponseDTO responseDTO = new IncomeResponseDTO();
        responseDTO.setIncomeId(income.getIncomeId());
        responseDTO.setAmount(income.getIncomeAmount());
        responseDTO.setIncomeDate(income.getIncomeDate());
        responseDTO.setIncomeTypeName(income.getIncomeType().getTypeName());
        responseDTO.setIncomeDescription(income.getIncomeDescription());

        // Convertir User a DTO anidado
        IncomeResponseDTO.IncomeUserDTO userDTO = new IncomeResponseDTO.IncomeUserDTO();
        userDTO.setUserId(income.getUser().getUserId());
        userDTO.setUsername(income.getUser().getUsername());
        responseDTO.setUser(userDTO);

        return responseDTO;
    }
	

}
