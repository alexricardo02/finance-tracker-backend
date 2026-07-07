package com.example.service;

import java.sql.Timestamp;


import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.PasswordChangeDTO;
import com.example.dataTransferObjects.UserProfileDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.dataTransferObjects.UserUpdateDTO;
import com.example.models.Expense;
import com.example.models.Income;
import com.example.models.Role;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import com.example.models.Category;

@Service
public class UserService {
	
	@Autowired
	private CategoryRepository categoryRepository;
	
	@Autowired // 
    private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	// Helper: Convert User → ProfileDTO
    private UserProfileDTO convertToProfileDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCreationDate(user.getCreation_date());
        return dto;
    }
    
    public Integer findUserIdByUsername(String username) {
    	return userRepository.findUserIdByUsername(username);
    }
	
	@Transactional
	public UserProfileDTO  saveUser(UserRegistrationDTO  registrationDTO) {
		if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
		// Validate username uniqueness
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        
     // Create new user
        User user = new User();
        user.setUsername(registrationDTO.getUsername());
        user.setEmail(registrationDTO.getEmail());
        user.setPassword_hash(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setCreation_date(new Timestamp(System.currentTimeMillis()));
        
        User savedUser = userRepository.save(user);
        
        java.util.List<Category> defaultCategories = java.util.Arrays.asList(
                new Category("Salary", "income", savedUser),
                new Category("Food", "expense", savedUser),
                new Category("Rent", "expense", savedUser),
                new Category("Transport", "expense", savedUser),
                new Category("Entertainment", "expense", savedUser)
            );
        categoryRepository.saveAll(defaultCategories);
        
        return convertToProfileDTO(savedUser);
        
    }
	
	public Role getRoleByUsername(String username) {
	    return userRepository.findByUsername(username)
	            .orElseThrow(() -> new EntityNotFoundException("User not found"))
	            .getRole();
	}
	
	public List<User> getAllUsers() {
		return userRepository.findAll();
	};
	
	public UserProfileDTO getUserById(Integer id) {
		User user = userRepository.findById(id)
	            .orElseThrow(() -> new EntityNotFoundException("User not found"));
	        return convertToProfileDTO(user);
	}
	
	@Transactional
	public void deleteUser(Integer id) {
		userRepository.deleteById(id);
	}
	
	
	// Update user profile (username/email)
    @Transactional
    public UserProfileDTO updateUserProfile(Integer userId, UserUpdateDTO updateDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Update username if provided and unique
        if (updateDTO != null && updateDTO.getUsername() != null 
                && !updateDTO.getUsername().isBlank()) {
            if (!user.getUsername().equals(updateDTO.getUsername())) {
                if (userRepository.existsByUsername(updateDTO.getUsername())) {
                    throw new IllegalArgumentException("Username already taken");
                }
                user.setUsername(updateDTO.getUsername());
            }
        }
        
        // Update email if provided and valid
        if (updateDTO.getEmail() != null) {
            if (!user.getEmail().equals(updateDTO.getEmail())) {
                if (userRepository.existsByEmail(updateDTO.getEmail())) {
                    throw new IllegalArgumentException("Email already registered");
                }
                if (!isValidEmail(updateDTO.getEmail())) {
                    throw new IllegalArgumentException("Invalid email format");
                }
                user.setEmail(updateDTO.getEmail());
            }
        }
        
        return convertToProfileDTO(userRepository.save(user));
       
    }
    
	
	public List<Expense> findExpensesByUserId(Integer id) {
		return userRepository.findExpensesByUserId(id);
	}
    
	public List<Income> findIncomesByUserId(Integer id) {
		return userRepository.findIncomesByUserId(id);
	}
	
	
	@Transactional
    public void updatePassword(Integer userId, PasswordChangeDTO passwordDTO) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword_hash())) {
            throw new SecurityException("Current password is incorrect");
        }
        
        // Update to new password
        user.setPassword_hash(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(user);
    }


	
	private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
	
	
	public UserProfileDTO login(LoginRequestDTO loginDTO) {
	    // 1. Buscamos al usuario por su username
	    // El repositorio devuelve un Optional<User>, por eso usamos .orElseThrow
	    User user = userRepository.findByUsername(loginDTO.getUsername())
	            .orElseThrow(() -> new SecurityException("Usuario o contraseña incorrectos"));

	    // 2. Comparamos la contraseña en texto plano con el hash de la base de datos
	    // Usamos el passwordEncoder que ya tienes inyectado
	    if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword_hash())) {
	        throw new SecurityException("Usuario o contraseña incorrectos");
	    }

	    // 3. Si la clave es correcta, convertimos el User a UserProfileDTO y lo devolvemos
	    return convertToProfileDTO(user);
	}
}
