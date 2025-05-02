package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import models.Income;

public interface IncomeRepository extends JpaRepository<Income, Integer>{

}
