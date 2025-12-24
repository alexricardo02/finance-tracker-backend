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
	
		// Obtener todas las incomes de un tipo
		@Query(
		        value = "SELECT * FROM incomes i " +
		                "WHERE i.type = :incomeTypeName",
		        nativeQuery = true // ¡Indica que es SQL!
		    )
		List<Income> findByIncomeTypeName(@Param("incomeTypeName") String incomeTypeName);
	
		
		// Obtener total de ingresos por type
		@Query(
				value = "SELECT SUM(i.amount) FROM incomes i " +
		                "WHERE i.type_name = :incomeTypeName",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByType(@Param("incomeTypeName") String incomeTypeName);
		
		
		//Get total amount of Incomes per month
		@Query(
				value = "SELECT SUM(i.amount) FROM incomes i " +
						"WHERE MONTH(i.date) = :monthNumber",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByMonth(@Param("monthNumber") Integer monthNumber);
		
		//Get total amount of Incomes per year
				@Query(
						value = "SELECT SUM(i.amount) FROM incomes i " +
								"WHERE YEAR(i.date) = :year",
						nativeQuery = true
						)
		Double getTotalIncomeAmountByYear(@Param("year") Integer year);
				
				
		//Get total amount of Incomes per day
				@Query(
						value = "SELECT SUM(i.amount) FROM incomes i " +
								"WHERE DAY(i.date) = :day",
						nativeQuery = true
						)
		Double getTotalIncomeAmountByDay(@Param("day") Integer day);
				
				
		@Query(
				value = "SELECT COALESCE(SUM(i.amount), 0) FROM incomes i " +
						"WHERE i.date BETWEEN :startDate AND :endDate",
		        nativeQuery = true // ¡Indica que es SQL!
				)
		Double getTotalIncomeAmountBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

		


}
