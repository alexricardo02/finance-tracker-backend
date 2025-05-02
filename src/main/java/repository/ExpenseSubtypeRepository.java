package repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import models.ExpenseSubtype;
import models.ExpenseType;

public interface ExpenseSubtypeRepository extends JpaRepository<ExpenseSubtype, Integer> {
	// Busca subtipos por nombre y tipo
    Optional<ExpenseSubtype> findBySubtypeNameAndExpenseType(String subtypeName, ExpenseType expenseType);

 // Lista todos los subtipos de un tipo específico
    List<ExpenseSubtype> findByExpenseType(ExpenseType expenseType);
}
