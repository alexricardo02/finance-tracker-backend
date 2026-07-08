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
import com.example.models.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer>, JpaSpecificationExecutor<Income>{
	
		Page<Income> findByUserUserId(int userId, Pageable pageable);
	
		// Obtener todas las incomes de un tipo
		@Query(
		        value = "SELECT i.* FROM incomes i " +
		                "JOIN categories c ON i.category_id = c.category_id " +
		                "WHERE c.name = :incomeTypeName AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		    )
		List<Income> findByIncomeTypeNameAndUser(@Param("incomeTypeName") String incomeTypeName, @Param("userId") Integer userId);
	
		
		// Obtener total de ingresos por type
		@Query(
		        value = "SELECT COALESCE(SUM(COALESCE(i.amount_primary_currency, i.amount)), 0) FROM incomes i " +
		                "JOIN categories c ON i.category_id = c.category_id " +
		                "WHERE c.name = :incomeTypeName AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		        )
		Double getTotalIncomeAmountByTypeAndUser(@Param("incomeTypeName") String incomeTypeName, @Param("userId") Integer userId);
		
		
		//Get total amount of Incomes per month
		@Query(
		        value = "SELECT COALESCE(SUM(COALESCE(i.amount_primary_currency, i.amount)), 0) FROM incomes i " +
		                "WHERE EXTRACT(MONTH FROM i.date) = :monthNumber AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		        )
		Double getTotalIncomeAmountByMonthAndUser(@Param("monthNumber") Integer monthNumber, @Param("userId") Integer userId);
		
		//Get total amount of Incomes per year
		@Query(
		        value = "SELECT COALESCE(SUM(COALESCE(i.amount_primary_currency, i.amount)), 0) FROM incomes i " +
		                "WHERE EXTRACT(YEAR FROM i.date) = :year AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		        )
		Double getTotalIncomeAmountByYearAndUser(@Param("year") Integer year, @Param("userId") Integer userId);
				
				
		//Get total amount of Incomes per day
		@Query(
		        value = "SELECT COALESCE(SUM(COALESCE(i.amount_primary_currency, i.amount)), 0) FROM incomes i " +
		                "WHERE EXTRACT(DAY FROM i.date) = :day AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		        )
		Double getTotalIncomeAmountByDayAndUser(@Param("day") Integer day, @Param("userId") Integer userId);
				
				
		@Query(
		        value = "SELECT COALESCE(SUM(COALESCE(i.amount_primary_currency, i.amount)), 0) FROM incomes i " +
		                "WHERE i.date BETWEEN :startDate AND :endDate AND i.user_id = :userId AND i.deleted_at IS NULL",
		        nativeQuery = true
		        )
		Double getTotalIncomeAmountBetweenAndUser(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") Integer userId);

		


}
