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
	
	// Buscar usuario por username (para login)
    Optional<User> findByUsername(String username);
    
    Integer findUserIdByUsername(String username);
   

    // Buscar usuario por email (para recuperación de contraseña)
    Optional<User> findByEmail(String email);

    // Verificar si un username ya existe (para registro)
    boolean existsByUsername(String username);

    // Verificar si un email ya existe (para registro)
    boolean existsByEmail(String email);
    
    // Obtener todos los gastos de un usuario
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId")
    List<Expense> findExpensesByUserId(@Param("userId") Integer userId);

    // Obtener todos los ingresos de un usuario
    @Query("SELECT i FROM Income i WHERE i.user.id = :userId")
    List<Income> findIncomesByUserId(@Param("userId") Integer userId);
    
    // Actualizar contraseña
    @Modifying 
    @Query(
	        value = "UPDATE users SET password_hash = :newPassword WHERE user_id = :userId",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
    void updatePassword(@Param("userId") Integer userId, @Param("newPassword") String newPassword);
    
    // Actualizar email
    @Modifying 
    @Query(
	        value = "UPDATE users SET email = :newEmail WHERE user_id = :userId",
	        nativeQuery = true // ¡Indica que es SQL!
	    )
    void updateEmail(@Param("userId") Integer userId, @Param("newEmail") String newEmail);

}
