package com.example.repository;

import com.example.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
	List<Category> findByUserUserIdOrUserIsNull(Integer userId);
    List<Category> findByUserIsNull();
}