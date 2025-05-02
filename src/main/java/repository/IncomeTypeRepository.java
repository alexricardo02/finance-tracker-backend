package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import models.IncomeType;

public interface IncomeTypeRepository extends JpaRepository<IncomeType, Integer>{

}
