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
import com.example.dataTransferObjects.TokenRefreshRequestDTO;
import com.example.dataTransferObjects.UserProfileDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.models.RefreshToken;
import com.example.models.User;
import com.example.security.JwtUtil;
import com.example.service.RefreshTokenService;
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
	
	@Autowired
    private RefreshTokenService refreshTokenService;
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
		UserProfileDTO userProfile = userService.login(loginDTO);
		String token = jwtUtil.generateToken(userProfile.getUsername(), userService.getRoleByUsername(userProfile.getUsername()));
	    RefreshToken refreshToken = refreshTokenService.createRefreshToken(userProfile.getUserId());
	    
	    LoginResponseDTO response = new LoginResponseDTO(token, refreshToken.getToken(), userProfile);
	    
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
		UserProfileDTO newUser = userService.saveUser(registrationDTO);
	    
	    return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequestDTO request) {
	    String requestRefreshToken = request.getRefreshToken();

	    RefreshToken tokenDB = refreshTokenService.findByToken(requestRefreshToken)
	        .orElseThrow(() -> new SecurityException("Refresh token no encontrado en la base de datos"));

	    refreshTokenService.verifyExpiration(tokenDB);

	    User user = tokenDB.getUser();
	    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());

	    // ROTACIÓN: invalida el refresh token usado y emite uno nuevo
	    RefreshToken rotated = refreshTokenService.rotateRefreshToken(tokenDB);

	    LoginResponseDTO response = new LoginResponseDTO(newAccessToken, rotated.getToken(), null);
	    return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@Valid @RequestBody TokenRefreshRequestDTO request) {
	    refreshTokenService.deleteByToken(request.getRefreshToken());
	    return ResponseEntity.noContent().build();
	}
	
	
}
