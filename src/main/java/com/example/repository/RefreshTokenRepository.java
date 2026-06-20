package com.example.repository;

import com.example.models.RefreshToken;
import org.springframework.data.jpa.repository.Query;
import com.example.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer>{
	
    Optional<RefreshToken> findByToken(String token);
    
    @Query("SELECT r FROM RefreshToken r WHERE r.user.userId = :userId")
    Optional<RefreshToken> findByUserId(@Param("userId") int userId);
    
    void deleteByUser(User user);

}
