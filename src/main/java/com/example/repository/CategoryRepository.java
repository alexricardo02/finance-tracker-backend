package com.example.repository;

import com.example.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
	List<Category> findByUserUserIdOrUserIsNull(Integer userId);
    List<Category> findByUserIsNull();
    
    @Modifying
    @Query("DELETE FROM Category c WHERE c.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Integer userId);
}