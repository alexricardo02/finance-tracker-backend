package com.example.service;


import java.time.LocalDate;
import java.util.List;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.models.Income;
import com.example.models.User;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class IncomeService {
	
	@Autowired 
    private IncomeRepository incomeRepository;
	
    @Autowired
    private UserRepository userRepository;
  
    
	
	private IncomeResponseDTO convertToResponseDTO(Income income) {
		
		return new IncomeResponseDTO(
				income.getIncomeId(),
				income.getAmount(),
				income.getCurrency(),
				income.getDate(),
				income.getType(),
				income.getDescription(),
				income.getUser().getUserId()
				);
			
    }
	
	@Transactional
	public IncomeResponseDTO saveIncome(IncomeRequestDTO requestDTO) {
		
		if (requestDTO.getAmount() == null) {
	        throw new IllegalArgumentException("Amount is required");
	    }
	
		
		// 1. Convertir DTO a entidad
        Income income = new Income();
        income.setAmount(requestDTO.getAmount());
        income.setCurrency(requestDTO.getCurrency());
        income.setDate(requestDTO.getDate());
        income.setType(requestDTO.getType());
        income.setDescription(requestDTO.getDescription());
      
        
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
            income.setUser(user);
        
        
        // 3. Guardar en la base de datos
        Income savedIncome = incomeRepository.save(income);
        
        // 4. Convertir entidad a ResponseDTO
        return convertToResponseDTO(savedIncome);
    }

	
	public List<IncomeResponseDTO> getAllIncomes() {
		List<Income> incomes = incomeRepository.findAll();
		return incomes.stream()
	            .map(income -> convertToResponseDTO(income))
	            .toList();
	};
	
	public IncomeResponseDTO  getIncomeById(Integer id) {
		
		Income income = incomeRepository.findById(id)
		        .orElseThrow(() -> new EntityNotFoundException("Income not found with ID: " + id));
		return convertToResponseDTO(income);
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
		existingIncome.setAmount(incomeRequestDTO.getAmount());
		existingIncome.setDate(incomeRequestDTO.getDate());
		existingIncome.setDescription(incomeRequestDTO.getDescription());
		existingIncome.setType(incomeRequestDTO.getType());
		existingIncome.setCurrency(incomeRequestDTO.getCurrency());
		
        
		
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

	
	public List<IncomeResponseDTO> findByIncomeTypeName(String incomeType) {
		List<Income> incomes = incomeRepository.findByIncomeTypeName(incomeType);
	    return incomes.stream()
	            .map(income -> convertToResponseDTO(income))
	            .toList();
	}
	
	public Double getTotalIncomeAmountByType(String incomeType) {
		return incomeRepository.getTotalIncomeAmountByType(incomeType);
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
	
	public Double getTotalIncomeAmounByDay(Integer day) {
    	switch(day) {
    	case 1: 
    		return incomeRepository.getTotalIncomeAmountByDay(1);
    	case 2: 
    		return incomeRepository.getTotalIncomeAmountByDay(2);
    	case 3: 
    		return incomeRepository.getTotalIncomeAmountByDay(3);
    	case 4: 
    		return incomeRepository.getTotalIncomeAmountByDay(4);
    	case 5: 
    		return incomeRepository.getTotalIncomeAmountByDay(5);
    	case 6: 
    		return incomeRepository.getTotalIncomeAmountByDay(6);
    	case 7: 
    		return incomeRepository.getTotalIncomeAmountByDay(7);
    	default:
    		throw new IllegalArgumentException("Wrong day");
    	}
    }
	
	
	public Double getTotalIncomesLast7DaysInclusive(LocalDate today) {
    	LocalDate start = today.minusDays(6);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalIncomesLastMonths(LocalDate today) {
    	LocalDate start = today.minusMonths(1);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalIncomesLast3Months(LocalDate today) {
    	LocalDate start = today.minusMonths(3);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalIncomesLast6Months(LocalDate today) {
    	LocalDate start = today.minusMonths(6);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }
    
    public Double getTotalIncomesLastYear(LocalDate today) {
    	LocalDate start = today.minusYears(1);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetween(start, end);
    	return result == null ? 0.0 : result;
    }   


	

}
