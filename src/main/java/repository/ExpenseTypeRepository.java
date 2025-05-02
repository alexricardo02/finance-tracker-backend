package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import models.ExpenseType;

@Repository
public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Integer> {
	
	ExpenseType findByTypeName(String expenseType);

	boolean existsByTypeName(String expenseTypeName);
}
