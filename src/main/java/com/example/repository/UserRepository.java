package com.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{
	
  // Find user by username (for login)
    Optional<User> findByUsername(String username);
    
    Integer findUserIdByUsername(String username);
   

      // Find user by email (for password recovery)
    Optional<User> findByEmail(String email);

      // Check whether a username already exists (for registration)
    boolean existsByUsername(String username);

      // Check whether an email already exists (for registration)
    boolean existsByEmail(String email);
    
      // Retrieve all expenses for a user
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId")
    List<Expense> findExpensesByUserId(@Param("userId") Integer userId);

      // Retrieve all incomes for a user
    @Query("SELECT i FROM Income i WHERE i.user.id = :userId")
    List<Income> findIncomesByUserId(@Param("userId") Integer userId);
    
      // Update password
    @Modifying 
    @Query(
	        value = "UPDATE users SET password_hash = :newPassword WHERE user_id = :userId",
          nativeQuery = true // Indicates that this is SQL
	    )
    void updatePassword(@Param("userId") Integer userId, @Param("newPassword") String newPassword);
    
      // Update email
    @Modifying 
    @Query(
	        value = "UPDATE users SET email = :newEmail WHERE user_id = :userId",
          nativeQuery = true // Indicates that this is SQL
	    )
    void updateEmail(@Param("userId") Integer userId, @Param("newEmail") String newEmail);

}
