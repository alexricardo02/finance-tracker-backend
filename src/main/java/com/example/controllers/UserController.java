package com.example.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.LoginResponseDTO;
import com.example.dataTransferObjects.UserProfileDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.security.JwtUtil;
import com.example.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
	
	@Autowired
    private UserService userService;
	
	@Autowired
    private JwtUtil jwtUtil;
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
	    try {
	        // Llamamos al servicio para validar credenciales
	        UserProfileDTO userProfile = userService.login(loginDTO);
	        
	     // 2. Si es correcto, generamos el Token usando el username
            String token = jwtUtil.generateToken(userProfile.getUsername());
            
         // 3. Empaquetamos todo en nuestro nuevo DTO
            LoginResponseDTO response = new LoginResponseDTO(token, userProfile);
	        
	        // Por ahora devolvemos el perfil. 
	        // El siguiente paso será devolver el TOKEN (JWT) aquí.
	        return ResponseEntity.ok(response);
	        
	    } catch (SecurityException e) {
	        // Si las credenciales fallan, devolvemos un 401 (No autorizado)
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
	    } catch (Exception e) {
	        // 4. Captura cualquier otro error inesperado (500)
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("Ocurrió un error inesperado");
	    }
	}
	
	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
	    try {
	        // Llamamos al método que ya tenías en el Service
	        UserProfileDTO newUser = userService.saveUser(registrationDTO);
	        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
	    } catch (IllegalArgumentException e) {
	        // Captura errores como "Email ya registrado" o "Usuario tomado"
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el registro");
	    }
	}
	
	

}
