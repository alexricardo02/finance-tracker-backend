package com.example.service;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.service.CacheService;
import com.example.dataTransferObjects.ExpenseRequestDTO;
import com.example.dataTransferObjects.ExpenseResponseDTO;
import com.example.dataTransferObjects.PagedResponse;
import com.example.models.Category;
import com.example.models.Expense;
import com.example.models.PaymentMethod;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class ExpenseService {

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private CacheService cacheService;

	@Autowired
	UserRepository userRepository;

	private ExpenseResponseDTO convertToResponseDTO(Expense expense) {
		ExpenseResponseDTO dto = new ExpenseResponseDTO();
		dto.setId(expense.getExpenseID());
		dto.setAmount(expense.getExpenseAmount());
		dto.setCurrency(expense.getCurrency());
		dto.setDate(expense.getExpenseDate());

		if (expense.getCategory() != null) {
			dto.setCategoryId(expense.getCategory().getCategoryId());
			dto.setCategoryName(expense.getCategory().getName());
		} else {
			dto.setCategoryName("Sin Categoría");
		}

		dto.setDescription(expense.getExpenseDescription());
		if (expense.getUser() != null) {
			dto.setUserId(expense.getUser().getUserId());
		}
		dto.setPaymentMethod(expense.getPaymentMethod());

		return dto;
	}

	public PagedResponse<ExpenseResponseDTO> getFilteredExpenses(String username, LocalDate startDate,
			LocalDate endDate, Long categoryId, PaymentMethod method, int page, int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

		Specification<Expense> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// WHY: Links the transaction to the principal.getName() passed from the
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

		Page<ExpenseResponseDTO> expensesPage = expenseRepository.findAll(spec, pageable)
				.map(this::convertToResponseDTO);

		// WHY: Explicitly matching the exact 6-parameter constructor of PagedResponse
		// resolves
		// the generic inference failure and maps all pagination metadata accurately.
		return new PagedResponse<>(expensesPage.getContent(), expensesPage.getNumber(), expensesPage.getSize(),
				expensesPage.getTotalElements(), expensesPage.getTotalPages(), expensesPage.isLast());
	}

	private Integer getUserIdByUsername(String username) {

		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"))
				.getUserId();
	}

	// @Cacheable(value = "user_expenses", key = "#username + '-' + #page + '-' +
	// #size")
	public PagedResponse<ExpenseResponseDTO> getExpensesForCurrentUserPaginated(String username, int page, int size) {

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

		Page<Expense> expensesPage = expenseRepository.findByUserUserId(user.getUserId(), pageable);

		List<ExpenseResponseDTO> dtoContent = expensesPage.getContent().stream().map(this::convertToResponseDTO)
				.collect(Collectors.toList());

		return new PagedResponse<>(dtoContent, expensesPage.getNumber(), expensesPage.getSize(),
				expensesPage.getTotalElements(), expensesPage.getTotalPages(), expensesPage.isLast());
	}

	@Transactional
	public ExpenseResponseDTO saveExpense(ExpenseRequestDTO requestDTO, String username) {

		if (requestDTO == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}

		if (requestDTO.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
		}

		if (requestDTO.getCategoryId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CategoryId is required");
		}

		Category category = categoryRepository.findById(requestDTO.getCategoryId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

		Expense expense = new Expense();
		expense.setExpenseAmount(requestDTO.getAmount());
		expense.setCurrency(requestDTO.getCurrency());
		expense.setExpenseDate(requestDTO.getDate());
		expense.setExpenseDescription(requestDTO.getDescription());
		expense.setPaymentMethod(requestDTO.getPaymentMethod());
		expense.setCategory(category);

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		expense.setUser(user);

		Expense savedExpense = expenseRepository.save(expense);
		cacheService.evictUserFinancialCache(username);
		return convertToResponseDTO(savedExpense);
	}

	@Cacheable(value = "all_expenses_types", key = "'all'")
	public List<String> getAllExpenseTypes() {
		return expenseRepository.findAllExpenseTypes();
	}

	public ExpenseResponseDTO getExpenseById(int id, String username) {
		Expense expense = expenseRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));

		if (!expense.getUser().getUsername().equals(username)) {
			throw new SecurityException("No tienes permiso para ver este gasto");
		}

		return convertToResponseDTO(expense);
	}

	@Transactional
	public void deleteExpense(Integer id, String username) {

		Expense expense = expenseRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Egreso no encontrado con ID: " + id));

		if (!expense.getUser().getUsername().equals(username)) {
			throw new SecurityException("No tienes permiso para eliminar este egreso");
		}

		expense.setDeletedAt(java.time.Instant.now());
		expense.setDeletedBy(username);
		expenseRepository.save(expense);
		cacheService.evictUserFinancialCache(username);
	}

	@Transactional
	public ExpenseResponseDTO updateExpense(int expenseId, ExpenseRequestDTO requestDTO, String username) {

		Expense existingExpense = expenseRepository.findById(expenseId)
				.orElseThrow(() -> new IllegalArgumentException("Expense not found"));

		if (!existingExpense.getUser().getUsername().equals(username)) {
			throw new SecurityException("No tienes permiso para editar este gasto");
		}

		Category category = categoryRepository.findById(requestDTO.getCategoryId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

		existingExpense.setExpenseAmount(requestDTO.getAmount());
		existingExpense.setExpenseDate(requestDTO.getDate());
		existingExpense.setExpenseDescription(requestDTO.getDescription());
		existingExpense.setCategory(category);
		existingExpense.setPaymentMethod(requestDTO.getPaymentMethod());

		Expense updatedExpense = expenseRepository.save(existingExpense);
		cacheService.evictUserFinancialCache(username);
		return convertToResponseDTO(updatedExpense);
	}

	public List<ExpenseResponseDTO> getExpenseByTypeAndUser(String expenseType, String username) {

		if (expenseType == null) {
			throw new IllegalArgumentException("ExpenseType no puede ser nulo");
		}
		List<Expense> expenses = expenseRepository.findByExpenseTypeNameAndUser(expenseType,
				getUserIdByUsername(username));

		return expenses.stream().map(this::convertToResponseDTO).collect(Collectors.toList());
	}

	@Cacheable(value = "expenses_type", key = "#username + '_'+ #expenseType")
	public Double getTotalExpenseAmountByTypeAndUser(String expenseType, String username) {
		return expenseRepository.getTotalExpenseAmountByTypeAndUser(expenseType, getUserIdByUsername(username));
	}

	@Cacheable(value = "expenses_last_7_days", key = "#username + '_' + #today")
	public Double getTotalExpensesLast7DaysInclusiveAndUser(LocalDate today, String username) {
		LocalDate start = today.minusDays(6);
		LocalDate end = today;
		Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end,
				getUserIdByUsername(username));
		return result == null ? 0.0 : result;
	}

	@Cacheable(value = "expenses_last_months", key = "#username + '_' + #today")
	public Double getTotalExpensesLastMonthsAndUser(LocalDate today, String username) {
		LocalDate start = today.minusMonths(1);
		LocalDate end = today;
		Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end,
				getUserIdByUsername(username));
		return result == null ? 0.0 : result;
	}

	@Cacheable(value = "expenses_last_3_months", key = "#username + '_' + #today")
	public Double getTotalExpensesLast3MonthsAndUser(LocalDate today, String username) {
		LocalDate start = today.minusMonths(3);
		LocalDate end = today;
		Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end,
				getUserIdByUsername(username));
		return result == null ? 0.0 : result;
	}

	@Cacheable(value = "expenses_last_6_months", key = "#username + '_' + #today")
	public Double getTotalExpensesLast6MonthsAndUser(LocalDate today, String username) {
		LocalDate start = today.minusMonths(6);
		LocalDate end = today;
		Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end,
				getUserIdByUsername(username));
		return result == null ? 0.0 : result;
	}

	@Cacheable(value = "expenses_last_year", key = "#username + '_' + #today")
	public Double getTotalExpensesLastYearAndUser(LocalDate today, String username) {
		LocalDate start = today.minusYears(1);
		LocalDate end = today;
		Double result = expenseRepository.getTotalExpenseAmountBetweenAndUser(start, end,
				getUserIdByUsername(username));
		return result == null ? 0.0 : result;
	}

	// Redis
	@Cacheable(value = "expenses_day", key = "#username + '_'+ #day")
	public Double getTotalExpenseAmounByDayAndUser(Integer day, String username) {

		if (day == null || day < 1 || day > 31) {
			throw new IllegalArgumentException("Invalid day");
		}

		return expenseRepository.getTotalExpenseAmountByDayAndUser(day, getUserIdByUsername(username));

	}

	// Redis
	@Cacheable(value = "expenses_month", key = "#username + '_' + #month")
	public Double getTotalExpenseAmounByMonthAndUser(String month, String username) {

		try {
			int monthNumber = java.time.Month.valueOf(month.toUpperCase()).getValue();
			return expenseRepository.getTotalExpenseAmountByMonthAndUser(monthNumber, getUserIdByUsername(username));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid month: " + month);
		}
	}

	// Redis
	@Cacheable(value = "expenses_year", key = "#username + '_' + #year")
	public Double getTotalExpenseAmounByYearAndUser(Integer year, String username) {
		if (year == null) {
			throw new IllegalArgumentException("El año no puede ser nulo");
		}
		return expenseRepository.getTotalExpenseAmountByYearAndUser(year, getUserIdByUsername(username));
	}

}
