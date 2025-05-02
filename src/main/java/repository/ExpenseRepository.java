package repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import models.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer>{

	
	@Query(
	        value = "SELECT * FROM expenses e " +
	                "JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
	                "JOIN expensetypes et ON es.type_id = et.type_id " +
	                "WHERE et.type_name = :expenseTypeName",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
	    List<Expense> findByExpenseTypeName(@Param("expenseTypeName") String expenseTypeName);
	
		
	@Query(
			value = "SELECT * FROM expenses e " +
					"JOIN expensesubtypes es ON e.subtype_id = es.subtype_id " +
					"WHERE es.subtype_name = :expenseSubtypeName",
					nativeQuery = true
			)
	List<Expense> findByExpenseSubtypeName(@Param("expenseSubtypeName") String expenseSubtypeName);
	
	
	//
	
	//
	
	//
	
	//
	

}
