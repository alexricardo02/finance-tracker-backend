package service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import models.Expense;
import models.Income;
import models.User;
import repository.UserRepository;

@Service
public class UserService {
	
	@Autowired // 
    private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Transactional
	public User saveUser(User user) {
		if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (user.getEmail() == null || !isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        return userRepository.save(user); // Usa el repositorio para guardar
    }
	
	public List<User> getAllUsers() {
		return userRepository.findAll();
	};
	
	public User getUserById(Integer id) {
		return userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User no encontrado"));
	}
	
	@Transactional
	public void deleteUser(Integer id) {
		User user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
		userRepository.delete(user);
	}
	
	@Transactional
	public User updateUser(User user) {
		// Verifica que el ingreso exista
		User existingUser = userRepository.findById(user.getUserId())
	            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + user.getUserId()));
        
		// Validar y actualizar campos no sensibles
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario no puede modificarse");
        }
        existingUser.setEmail(user.getEmail()); // Validar email en un método separado si es necesario
        existingUser.setCreation_date(user.getCreation_date()); // Omitir si no debe actualizarse

        return userRepository.save(existingUser);
	}
	
	public List<Expense> findExpensesByUserId(Integer id) {
		return userRepository.findExpensesByUserId(id);
	}
    
	public List<Income> findIncomesByUserId(Integer id) {
		return userRepository.findIncomesByUserId(id);
	}
	
	@Transactional
	public void updatePassword(Integer userId, String newPassword) {
		User user = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
		String encryptedPassword = passwordEncoder.encode(newPassword);
		user.setPassword_hash(encryptedPassword);
        userRepository.save(user);
	}
	
	@Transactional
	public void updateEmail(Integer userId, String newEmail) {
		if (!isValidEmail(newEmail)) {
            throw new IllegalArgumentException("El email no es válido");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
        user.setEmail(newEmail);
        userRepository.save(user);
	}
	
	private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
