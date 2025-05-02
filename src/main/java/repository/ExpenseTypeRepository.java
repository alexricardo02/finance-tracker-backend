package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import models.ExpenseType;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Integer> {
	
	ExpenseType findByTypeName(String expenseType);
	
}
