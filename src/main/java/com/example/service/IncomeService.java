package com.example.service;

import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import java.time.LocalDate;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.dataTransferObjects.IncomeRequestDTO;
import com.example.dataTransferObjects.IncomeResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.exceptions.ForbiddenResourceException;
import com.example.models.Category;
import com.example.models.Income;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;


@Service
public class IncomeService {
	
	@Autowired
	private ExchangeRateService exchangeRateService;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private IncomeRepository incomeRepository;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
    private CacheService cacheService;
	
	@Autowired
	private MeterRegistry meterRegistry;

	private IncomeResponseDTO convertToResponseDTO(Income income) {
		IncomeResponseDTO dto = new IncomeResponseDTO();
		dto.setIncomeId(income.getIncomeId());
		dto.setAmount(income.getAmount());
		dto.setCurrency(income.getCurrency());
		dto.setDate(income.getDate());
		
		if (income.getCategory() != null) {
			dto.setCategoryId(income.getCategory().getCategoryId());
			dto.setCategoryName(income.getCategory().getName());
		} else {
			dto.setCategoryName("Sin Categoría");
		}
		
		dto.setDescription(income.getDescription());
		if (income.getUser() != null) {
			dto.setUserId(income.getUser().getUserId());
		}
		dto.setPaymentMethod(income.getPaymentMethod());
		
		dto.setAmountPrimaryCurrency(income.getAmountPrimaryCurrency());
		if (income.getUser() != null) {
		    dto.setPrimaryCurrency(income.getUser().getPrimaryCurrency());
		}
		
		return dto;
	}

	@Transactional(readOnly = true)
	public PagedResponse<IncomeResponseDTO> getFilteredIncomes(String username, LocalDate startDate, LocalDate endDate,
			Long categoryId, PaymentMethod method, int page, int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

		Specification<Income> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Links the transaction to the principal.getName() passed from the
			// controller
			predicates.add(cb.equal(root.get("user").get("username"), username));

			if (startDate != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
			}
			if (endDate != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
			}
			if (categoryId != null) {
				predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
			}
			if (method != null) {
				predicates.add(cb.equal(root.get("paymentMethod"), method));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<IncomeResponseDTO> incomesPage = incomeRepository.findAll(spec, pageable).map(this::convertToResponseDTO);

		// Explicitly matching the exact 6-parameter constructor of PagedResponse
		// resolves
		// the generic inference failure and maps all pagination metadata accurately.
		return new PagedResponse<>(incomesPage.getContent(), incomesPage.getNumber(), incomesPage.getSize(),
				incomesPage.getTotalElements(), incomesPage.getTotalPages(), incomesPage.isLast());
	}

	private Integer getUserIdByUsername(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"))
				.getUserId();
	}

	@Transactional
	public IncomeResponseDTO saveIncome(IncomeRequestDTO requestDTO, String username) {

		if (requestDTO.getAmount() == null) {
			throw new BadRequestException("Amount is required");
		}

		if (requestDTO.getCategoryId() == null) {
			throw new BadRequestException("CategoryId is required");
		}

		Category category = categoryRepository.findById(requestDTO.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));

		if (!category.isUsableBy(username)) {
			throw new ForbiddenResourceException("Not authorized to use this category");
		}

		Income income = new Income();
		income.setAmount(requestDTO.getAmount());
		income.setCurrency(requestDTO.getCurrency());
		income.setDate(requestDTO.getDate());
		income.setDescription(requestDTO.getDescription());
		income.setCategory(category);
		income.setPaymentMethod(requestDTO.getPaymentMethod());

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		income.setUser(user);

		income.setUser(user);
		String primaryCurrency = user.getPrimaryCurrency();
		double rate = exchangeRateService.getConversionRate(requestDTO.getCurrency(), primaryCurrency, requestDTO.getDate());
		income.setAmountPrimaryCurrency(requestDTO.getAmount() * rate);
		Income savedIncome = incomeRepository.save(income);
		
		cacheService.evictUserFinancialCache(username);
		
		meterRegistry.counter("finance.transactions.created", "type", "income", "currency", requestDTO.getCurrency()).increment();
		
		return convertToResponseDTO(savedIncome);
	}

	// @Cacheable(value = "user_incomes", key = "#username + '-' + #page + '-' +
	// #size")
	@Transactional(readOnly = true)
	public PagedResponse<IncomeResponseDTO> getIncomesForCurrentUserPaginated(String username, int page, int size) {

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

		Page<Income> incomesPage = incomeRepository.findByUserUserId(user.getUserId(), pageable);

		List<IncomeResponseDTO> dtoContent = incomesPage.getContent().stream().map(this::convertToResponseDTO)
				.collect(Collectors.toList());

		return new PagedResponse<>(dtoContent, incomesPage.getNumber(), incomesPage.getSize(),
				incomesPage.getTotalElements(), incomesPage.getTotalPages(), incomesPage.isLast());
	}

	@Transactional(readOnly = true)
	public IncomeResponseDTO getIncomeById(Integer id, String username) {

		Income income = incomeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Income not found"));

		if (!income.getUser().getUsername().equals(username)) {
			throw new ForbiddenResourceException("Not authorized to view this income");
		}

		return convertToResponseDTO(income);
	}

	@Transactional
	public void deleteIncome(Integer id, String username) {

	    Income income = incomeRepository.findById(id)
	            .orElseThrow(() -> new ResourceNotFoundException("Income not found"));

	    if (!income.getUser().getUsername().equals(username)) {
	        throw new ForbiddenResourceException("Not authorized to delete this income");
	    }

	    income.setDeletedAt(java.time.Instant.now());
	    income.setDeletedBy(username);
	    incomeRepository.save(income);
	    cacheService.evictUserFinancialCache(username);
	}

	@Transactional
	public IncomeResponseDTO updateIncome(Integer incomeId, IncomeRequestDTO incomeRequestDTO, String username) {

		Income existingIncome = incomeRepository.findById(incomeId)
				.orElseThrow(() -> new ResourceNotFoundException("Income not found"));

		if (!existingIncome.getUser().getUsername().equals(username)) {
			throw new ForbiddenResourceException("Not authorized to edit this income");
		}

		Category category = categoryRepository.findById(incomeRequestDTO.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));

		if (!category.isUsableBy(username)) {
			throw new ForbiddenResourceException("Not authorized to use this category");
		}

		existingIncome.setAmount(incomeRequestDTO.getAmount());
		existingIncome.setDate(incomeRequestDTO.getDate());
		existingIncome.setDescription(incomeRequestDTO.getDescription());
		existingIncome.setCurrency(incomeRequestDTO.getCurrency());
		existingIncome.setCategory(category);
		existingIncome.setPaymentMethod(incomeRequestDTO.getPaymentMethod());
		
		String primaryCurrency = existingIncome.getUser().getPrimaryCurrency();
		double rate = exchangeRateService.getConversionRate(incomeRequestDTO.getCurrency(), primaryCurrency, incomeRequestDTO.getDate());
		existingIncome.setAmountPrimaryCurrency(incomeRequestDTO.getAmount() * rate);

		Income updatedIncome = incomeRepository.save(existingIncome);
		
		cacheService.evictUserFinancialCache(username);

		return convertToResponseDTO(updatedIncome);

	}

	@Transactional(readOnly = true)
	public List<IncomeResponseDTO> findByIncomeTypeName(String incomeType, String username) {

		List<Income> incomes = incomeRepository.findByIncomeTypeNameAndUser(incomeType, getUserIdByUsername(username));
		return incomes.stream().map(income -> convertToResponseDTO(income)).toList();
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

		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid month");
		}
	}

	@Cacheable(value = "incomes_year", key = "#username + '_' + #year")
	public double getTotalIncomeAmountByYear(Integer year, String username) {
		if (year == null) {
			throw new BadRequestException("Year is required");
		}
		return incomeRepository.getTotalIncomeAmountByYearAndUser(year, getUserIdByUsername(username));
	}

	@Cacheable(value = "incomes_day", key = "#username + '_' + #day")
	public Double getTotalIncomeAmounByDay(Integer day, String username) {

		if (day == null || day < 1 || day > 31) {
			throw new BadRequestException("Invalid day");
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
