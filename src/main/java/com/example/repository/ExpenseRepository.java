package com.example.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.models.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer>, JpaSpecificationExecutor<Expense>{
	
	
	// NEW METHOD: return a page
    Page<Expense> findByUserUserId(int userId, Pageable pageable);

	// Obtener todas las expenses de un tipo
	@Query(
	        value = "SELECT * FROM expenses e " +
	                "WHERE e.type = :expenseTypeName AND e.user_id = :userId",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	List<Expense> findByExpenseTypeNameAndUser(@Param("expenseTypeName") String expenseTypeName, @Param("userId") Integer userId);
	
	@Query(
	        value = "SELECT DISTINCT e.type FROM expenses e WHERE e.type IS NOT NULL",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	List<String> findAllExpenseTypes();
	
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE e.date BETWEEN :startDate AND :endDate AND e.user_id = :userId",
	        nativeQuery = true // ¡Indica que es SQL!
			)
	Double getTotalExpenseAmountBetweenAndUser(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") Integer userId);
	
	// obtener total de gasto por type
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE e.type = :expenseTypeName AND e.user_id = :userId",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByTypeAndUser(@Param("expenseTypeName") String expenseTypeName, @Param("userId") Integer userId);
	
	
	//Get total amount of Expenses per month
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE MONTH(e.date) = :monthNumber AND e.user_id = :userId",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByMonthAndUser(@Param("monthNumber") Integer monthNumber, @Param("userId") Integer userId);
	
	//Get total amount of Expenses per year
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE YEAR(e.date) = :year AND e.user_id = :userId",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByYearAndUser(@Param("year") Integer year, @Param("userId") Integer userId);
	
	
	//Get total amount of Expenses per day
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE DAY(e.date) = :day AND e.user_id = :userId",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByDayAndUser(@Param("day") Integer day, @Param("userId") Integer userId);


}
