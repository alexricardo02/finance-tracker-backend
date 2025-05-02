package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import models.Expense;
import models.Income;

public interface IncomeRepository extends JpaRepository<Income, Integer>{
	
		// Obtener todas las incomes de un tipo
		@Query(
		        value = "SELECT * FROM incomes i " +
		                "JOIN incometypes it ON i.type_id = it.type_id " +
		                "WHERE it.type_name = :incomeTypeName",
		        nativeQuery = true // ¡Indica que es SQL!
		    )
		List<Expense> findByIncomeTypeName(@Param("incomeTypeName") String incomeTypeName);
	
		
		// Obtener total de ingresos por type
		@Query(
				value = "SELECT SUM(i.amount) FROM incomes i " +
						"JOIN incometypes it ON i.type_id = it.type_id " +
		                "WHERE it.type_name = :incomeTypeName",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByType(@Param("incomeTypeName") String incomeTypeName);
		
		
		// obtener total por mes
		@Query(
				value = "SELECT SUM(i.amount) FROM incomes i " +
						"WHERE MONTH(e.date) = :monthNumber",
				nativeQuery = true
				)
		Double getTotalIncomeAmountByMonth(@Param("monthNumber") Integer monthNumber);

}
