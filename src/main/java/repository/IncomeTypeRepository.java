package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import models.IncomeType;

@Repository
public interface IncomeTypeRepository extends JpaRepository<IncomeType, Integer>{
	
	boolean incomeTypeExistsById(Integer id);

}
