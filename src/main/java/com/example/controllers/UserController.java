package com.example.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.dataTransferObjects.DeleteAccountRequestDTO;

import com.example.dataTransferObjects.ForgotPasswordRequestDTO;
import com.example.dataTransferObjects.ResetPasswordRequestDTO;
import com.example.service.PasswordResetService;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.UserProfileDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.models.RefreshToken;
import com.example.models.User;
import com.example.security.CookieUtil;
import com.example.security.JwtUtil;
import com.example.security.TokenBlacklistService;
import com.example.service.RefreshTokenService;
import com.example.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired private PasswordResetService passwordResetService;
    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CookieUtil cookieUtil;
    @Autowired private TokenBlacklistService tokenBlacklistService;
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        UserProfileDTO userProfile = userService.login(loginDTO);
        String token = jwtUtil.generateToken(userProfile.getUsername(),
                userService.getRoleByUsername(userProfile.getUsername()));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userProfile.getUserId());

        ResponseCookie accessCookie = cookieUtil.buildAccessTokenCookie(token, jwtUtil.getExpirationSeconds());
        ResponseCookie refreshCookie = cookieUtil.buildRefreshTokenCookie(refreshToken.getToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(userProfile);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserProfileDTO newUser = userService.saveUser(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String requestRefreshToken = extractCookie(request, "refresh_token");
        if (requestRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken tokenDB = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new SecurityException("Refresh token no encontrado"));
        refreshTokenService.verifyExpiration(tokenDB);

        User user = tokenDB.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(tokenDB);

        ResponseCookie accessCookie = cookieUtil.buildAccessTokenCookie(newAccessToken, jwtUtil.getExpirationSeconds());
        ResponseCookie refreshCookie = cookieUtil.buildRefreshTokenCookie(rotated.getToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String accessToken = extractCookie(request, "auth_token");
        String refreshTokenValue = extractCookie(request, "refresh_token");

        if (accessToken != null) {
            try {
                tokenBlacklistService.blacklist(jwtUtil.extractJti(accessToken),
                        jwtUtil.getRemainingValiditySeconds(accessToken));
            } catch (Exception ignored) {}
        }
        if (refreshTokenValue != null) {
            refreshTokenService.deleteByToken(refreshTokenValue);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("auth_token").toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("refresh_token").toString())
                .build();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
    
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(Principal principal,
                                            @Valid @RequestBody DeleteAccountRequestDTO dto,
                                            HttpServletRequest request) {
        userService.deleteAccount(principal.getName(), dto.getPassword());

        String accessToken = extractCookie(request, "auth_token");
        if (accessToken != null) {
            try {
                tokenBlacklistService.blacklist(jwtUtil.extractJti(accessToken),
                        jwtUtil.getRemainingValiditySeconds(accessToken));
            } catch (Exception ignored) {}
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("auth_token").toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearCookie("refresh_token").toString())
                .build();
    }
}
