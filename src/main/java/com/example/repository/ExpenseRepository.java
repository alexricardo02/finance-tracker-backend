package com.example.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.models.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer>{

	// Obtener todas las expenses de un tipo
	@Query(
	        value = "SELECT * FROM expenses e " +
	                "WHERE e.type = :expenseTypeName",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	List<Expense> findByExpenseTypeName(@Param("expenseTypeName") String expenseTypeName);
	
	@Query(
	        value = "SELECT DISTINCT e.type FROM expenses e WHERE e.type IS NOT NULL",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	List<String> findAllExpenseTypes();
	
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE e.date BETWEEN :startDate AND :endDate",
	        nativeQuery = true // ¡Indica que es SQL!
			)
	Double getTotalExpenseAmountBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
	
	// obtener total de gasto por type
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE e.type = :expenseTypeName",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByType(@Param("expenseTypeName") String expenseTypeName);
	
	
	//Get total amount of Expenses per month
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE MONTH(e.date) = :monthNumber",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByMonth(@Param("monthNumber") Integer monthNumber);
	
	//Get total amount of Expenses per year
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE YEAR(e.date) = :year",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByYear(@Param("year") Integer year);
	
	
	//Get total amount of Expenses per day
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE DAY(e.date) = :day",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByDay(@Param("day") Integer day);


}
