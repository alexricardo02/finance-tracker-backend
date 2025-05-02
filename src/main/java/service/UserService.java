package service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import models.Expense;
import models.Income;
import models.User;
import repository.IncomeRepository;
import repository.UserRepository;

public class UserService {
	
	@Autowired // 
    private UserRepository userRepository;
	
	@Transactional
	public User saveUser(User user) {
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
		userRepository.deleteById(id);
	}
	
	@Transactional
	public User updateUser(User user) {
		// Verifica que el ingreso exista
        if (!userRepository.existsById(user.getUserId())) {
            throw new RuntimeException("User no existe");
        }
        return userRepository.save(user);
	}
	
	public List<Expense> findExpensesByUserId(Integer id) {
		return userRepository.findExpensesByUserId(id);
	}
    
	public List<Income> findIncomesByUserId(Integer id) {
		return userRepository.findIncomesByUserId(id);
	}
	
	@Transactional
	public void updatePassword(Integer userId, String newPassword) {
		userRepository.updatePassword(userId, newPassword);
	}
	
	@Transactional
	public void updateEmail(Integer userId, String newEmail) {
		userRepository.updateEmail(userId, newEmail);
	}
}
