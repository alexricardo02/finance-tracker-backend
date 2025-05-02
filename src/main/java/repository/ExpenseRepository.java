package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import models.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer>{

	// Obtener todas las expenses de un tipo
	@Query(
	        value = "SELECT * FROM expenses e " +
	                "JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
	                "JOIN expensetypes et ON es.type_id = et.type_id " +
	                "WHERE et.type_name = :expenseTypeName",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	List<Expense> findByExpenseTypeName(@Param("expenseTypeName") String expenseTypeName);
	
		
	// Obtener todas las expenses de un subtipo
	@Query(
			value = "SELECT * FROM expenses e " +
					"JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
					"WHERE es.subtype_name = :expenseSubtypeName",
					nativeQuery = true
			)
	List<Expense> findByExpenseSubtypeName(@Param("expenseSubtypeName") String expenseSubtypeName);
	
	
	// obtener total de gasto por type
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
					"JOIN expensetypes et ON es.type_id = et.type_id " +
					"WHERE et.type_name = :expenseTypeName",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByType(@Param("expenseTypeName") String expenseTypeName);
	

	// obtener total de gasto por subtype
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
					"WHERE es.subtype_name = :expenseSubtypeName",
			nativeQuery = true
			)
	Double getTotalExpenseAmountBySubtype(@Param("expenseSubtypeName") String expenseSubtypeName);
	
	
	// obtener total por mes
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE MONTH(e.date) = :monthNumber",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByMonth(@Param("monthNumber") Integer monthNumber);
	
	// obtener total por año
	@Query(
			value = "SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
					"WHERE YEAR(e.date) = :year",
			nativeQuery = true
			)
	Double getTotalExpenseAmountByYear(@Param("year") Integer year);
	
	//Get total amount of Expenses per year
	
	//Get total amount of Expenses per month
	
	//Get total amount of Expenses per day
	
	//Expenses greater than
	
	//Expenses less than

}
