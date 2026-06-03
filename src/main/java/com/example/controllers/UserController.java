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
		UserProfileDTO userProfile = userService.login(loginDTO);
	    String token = jwtUtil.generateToken(userProfile.getUsername());
	    LoginResponseDTO response = new LoginResponseDTO(token, userProfile);
	    
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
		UserProfileDTO newUser = userService.saveUser(registrationDTO);
	    
	    return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
	}
	
	
}
