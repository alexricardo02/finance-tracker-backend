package com.example.repository;

import java.time.LocalDate;

import java.util.List; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.models.Income;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Integer>{
	
		List<Income> findByUserUserId(int userId);
	
		// Obtener todas las incomes de un tipo
		@Query(
		        value = "SELECT * FROM incomes i " +
		                "WHERE i.type = :incomeTypeName AND i.user_id = :userId",
		        nativeQuery = true // ¡Indica que es SQL!
		    )
		List<Income> findByIncomeTypeNameAndUser(@Param("incomeTypeName") String incomeTypeName, @Param("userId") Integer userId);
	
		
		// Obtener total de ingresos por type
		@Query(
				value = "SELECT COALESCE(SUM(i.amount)) FROM incomes i " +
		                "WHERE i.type = :incomeTypeName AND i.user_id = :userId",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByTypeAndUser(@Param("incomeTypeName") String incomeTypeName, @Param("userId") Integer userId);
		
		
		//Get total amount of Incomes per month
		@Query(
				value = "SELECT COALESCE(SUM(i.amount)) FROM incomes i " +
						"WHERE MONTH(i.date) = :monthNumber AND i.user_id = :userId",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByMonthAndUser(@Param("monthNumber") Integer monthNumber, @Param("userId") Integer userId);
		
		//Get total amount of Incomes per year
				@Query(
						value = "SELECT COALESCE(SUM(i.amount)) FROM incomes i " +
								"WHERE YEAR(i.date) = :year AND i.user_id = :userId",
						nativeQuery = true
						)
		Double getTotalIncomeAmountByYearAndUser(@Param("year") Integer year, @Param("userId") Integer userId);
				
				
		//Get total amount of Incomes per day
				@Query(
						value = "SELECT COALESCE(SUM(i.amount)) FROM incomes i " +
								"WHERE DAY(i.date) = :day AND i.user_id = :userId",
						nativeQuery = true
						)
		Double getTotalIncomeAmountByDayAndUser(@Param("day") Integer day, @Param("userId") Integer userId);
				
				
		@Query(
				value = "SELECT COALESCE(SUM(i.amount), 0) FROM incomes i " +
						"WHERE i.date BETWEEN :startDate AND :endDate AND i.user_id = :userId",
		        nativeQuery = true // ¡Indica que es SQL!
				)
		Double getTotalIncomeAmountBetweenAndUser(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") Integer userId);

		


}
