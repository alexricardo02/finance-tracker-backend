package com.example.service;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.models.Expense;
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
	
	private Integer getUserIdByUsername(String username) {
	    return userRepository.findByUsername(username)
	            .orElseThrow(() -> new RuntimeException("User not found"))
	            .getUserId();
	}
	
	@Transactional
	@CacheEvict(value = {"incomes_month", "incomes_type", "incomes_year", "incomes_day", "all_incomes", "incomes_last_7_days", "incomes_last_month", "incomes_last_3_month", "incomes_last_6_months", "incomes_last_year", "user_incomes"}, allEntries = true)	
	public IncomeResponseDTO saveIncome(IncomeRequestDTO requestDTO, String username) {
		
		if (requestDTO.getAmount() == null) {
	        throw new IllegalArgumentException("Amount is required");
	    }
	
	    System.out.println("LOG: Spring Security dice que el usuario es: " + username);
		
		// 1. Convertir DTO a entidad
        Income income = new Income();
        income.setAmount(requestDTO.getAmount());
        income.setCurrency(requestDTO.getCurrency());
        income.setDate(requestDTO.getDate());
        income.setType(requestDTO.getType());
        income.setDescription(requestDTO.getDescription());
      
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
            income.setUser(user);
            
        System.out.println("LOG: El ID de ese usuario en la DB es: " + user.getUserId());
        
        income.setUser(user);
        // 3. Guardar en la base de datos
        Income savedIncome = incomeRepository.save(income);
        
        // 4. Convertir entidad a ResponseDTO
        return convertToResponseDTO(savedIncome);
    }


	
	@Cacheable(value = "user_incomes", key = "#username + '-' + #page + '-' + #size")
	public PagedResponse<IncomeResponseDTO> getIncomesForCurrentUserPaginated(String username, int page, int size) {
        
    
        // 2. Buscamos a ese usuario
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Preparamos la paginación (Página 'page', tamaño 'size', ordenado por fecha descendente)
        // NOTA: Cambia "date" si tu columna de fecha en la clase Expense se llama diferente (ej. "createdAt", "fecha")
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // 4. Buscamos en el repositorio usando el método paginado
        // NOTA: Asegúrate de que el método en tu repositorio se llame findByUserUserId(Long id, Pageable p)
        Page<Income> incomesPage = incomeRepository.findByUserUserId(user.getUserId(), pageable);

        // 5. Convertimos la lista interna de Expenses a ExpenseResponseDTO
        List<IncomeResponseDTO> dtoContent = incomesPage.getContent().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        // 6. Empaquetamos todo en nuestra caja mágica de paginación
        return new PagedResponse<>(
                dtoContent,
                incomesPage.getNumber(),
                incomesPage.getSize(),
                incomesPage.getTotalElements(),
                incomesPage.getTotalPages(),
                incomesPage.isLast()
        );
    }
	
	public IncomeResponseDTO  getIncomeById(Integer id, String username) {
		
		Income income = incomeRepository.findById(id)
		        .orElseThrow(() -> new EntityNotFoundException("Income not found with ID: " + id));
		
		if (!income.getUser().getUsername().equals(username)) {
	        throw new SecurityException("No tienes permiso para ver este ingreso");
	    }
		
		return convertToResponseDTO(income);
	}
	
	@Transactional
	@CacheEvict(value = {"incomes_month", "incomes_type", "incomes_year", "incomes_day", "all_incomes", "incomes_last_7_days", "incomes_last_month", "incomes_last_3_month", "incomes_last_6_months", "incomes_last_year", "user_incomes"}, allEntries = true)	
	public void deleteIncome(Integer id, String username) {
		
		Income income = incomeRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Ingreso no encontrado con ID: " + id));
		
		if (!income.getUser().getUsername().equals(username)) {
	        throw new SecurityException("No tienes permiso para eliminar este ingreso");
	    }
		
		if (!incomeRepository.existsById(id)) {
            throw new RuntimeException("Ingreso no encontrado con ID: " + id);
        }
		incomeRepository.deleteById(id);
	}
	
	@Transactional
	@CacheEvict(value = {"incomes_month", "incomes_type", "incomes_year", "incomes_day", "all_incomes", "incomes_last_7_days", "incomes_last_month", "incomes_last_3_month", "incomes_last_6_months", "incomes_last_year", "user_incomes"}, allEntries = true)	
	public IncomeResponseDTO  updateIncome(Integer incomeId, IncomeRequestDTO incomeRequestDTO, String username) {
		
		// 1. Buscar el ingreso existente
		Income existingIncome = incomeRepository.findById(incomeId)
				.orElseThrow(() -> new RuntimeException("Ingreso no encontrado con ID: " + incomeId));
		
		if (!existingIncome.getUser().getUsername().equals(username)) {
		    throw new SecurityException("No tienes permiso para editar este ingreso");
		}
		
		// 2. Actualizar campos básicos
		existingIncome.setAmount(incomeRequestDTO.getAmount());
		existingIncome.setDate(incomeRequestDTO.getDate());
		existingIncome.setDescription(incomeRequestDTO.getDescription());
		existingIncome.setType(incomeRequestDTO.getType());
		existingIncome.setCurrency(incomeRequestDTO.getCurrency());
		
		
		// 4. Guardar cambios
        Income updatedIncome = incomeRepository.save(existingIncome);
        
        // 5. Convertir a DTO de respuesta
        return convertToResponseDTO(updatedIncome);
		
	}

	public List<IncomeResponseDTO> findByIncomeTypeName(String incomeType, String username) {
	    
		List<Income> incomes = incomeRepository.findByIncomeTypeNameAndUser(incomeType, getUserIdByUsername(username));
	    return incomes.stream()
	            .map(income -> convertToResponseDTO(income))
	            .toList();
	}
	
	@Cacheable(value = "incomes_type", key = "#username + '_' + #incomeType")
	public Double getTotalIncomeAmountByType(String incomeType, String username) {

		return incomeRepository.getTotalIncomeAmountByTypeAndUser(incomeType, getUserIdByUsername(username));
	}
	
	@Cacheable(value = "incomes_month", key = "#username + '_' + #month")
	public Double getTotalIncomeAmountByMonth(String month, String username) {

		try {
			int monthNumber = java.time.Month.valueOf(month.toUpperCase()).getValue();
			return incomeRepository.getTotalIncomeAmountByMonthAndUser(monthNumber, getUserIdByUsername(username));
			
		}catch (IllegalArgumentException e) {
	        throw new IllegalArgumentException("Invalid month: " + month);
	    }
	}
	
	@Cacheable(value = "incomes_year", key = "#username + '_' + #year")
	public double getTotalIncomeAmountByYear(Integer year, String username) {

		return incomeRepository.getTotalIncomeAmountByYearAndUser(year, getUserIdByUsername(username));
	}
	
	@Cacheable(value = "incomes_day", key = "#username + '_' + #day")
	public Double getTotalIncomeAmounByDay(Integer day, String username) {
	    
		if (day == null || day < 1 || day > 31) {
	        throw new IllegalArgumentException("Invalid day");
	    }
	    
	    return incomeRepository.getTotalIncomeAmountByDayAndUser(day, getUserIdByUsername(username));
	}
	
	@Cacheable(value = "incomes_last_7_days", key = "#username + '_' + #today")
	public Double getTotalIncomesLast7DaysInclusive(LocalDate today, String username) {

    	LocalDate start = today.minusDays(6);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
	@Cacheable(value = "incomes_last_month", key = "#username + '_' + #today")
    public Double getTotalIncomesLastMonths(LocalDate today, String username) {

    	LocalDate start = today.minusMonths(1);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
	@Cacheable(value = "incomes_last_3_month", key = "#username + '_' + #today")
    public Double getTotalIncomesLast3Months(LocalDate today, String username) {

    	LocalDate start = today.minusMonths(3);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value = "incomes_last_6_months", key = "#username + '_' + #today")
    public Double getTotalIncomesLast6Months(LocalDate today, String username) {

    	LocalDate start = today.minusMonths(6);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }
    
    @Cacheable(value = "incomes_last_year", key = "#username + '_' + #today")
    public Double getTotalIncomesLastYear(LocalDate today, String username) {

    	LocalDate start = today.minusYears(1);
    	LocalDate end = today;
    	Double result = incomeRepository.getTotalIncomeAmountBetweenAndUser(start, end, getUserIdByUsername(username));
    	return result == null ? 0.0 : result;
    }   


	

}
