package com.example.repository;

import com.example.models.RefreshToken;
import com.example.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer>{
	
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByUserId(int userId);
    
    void deleteByUser(User user);

}
