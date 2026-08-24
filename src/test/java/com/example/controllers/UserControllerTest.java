package com.example.controllers;

import com.example.dataTransferObjects.*;
import com.example.models.RefreshToken;
import com.example.models.Role;
import com.example.models.User;
import com.example.security.CookieUtil;
import com.example.security.JwtUtil;
import com.example.security.TokenBlacklistService;
import com.example.service.PasswordResetService;
import com.example.service.RefreshTokenService;
import com.example.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private CookieUtil cookieUtil;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private PasswordResetService passwordResetService;

    @InjectMocks private UserController userController;

    private UserProfileDTO profile;
    private RefreshToken refreshToken;
    private ResponseCookie accessCookie;
    private ResponseCookie refreshCookie;
    private ResponseCookie clearCookie;
    private User user;

    @BeforeEach
    void setUp() {
        profile = new UserProfileDTO(1, "john", "john@test.com", new Date());

        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);

        refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("refresh-uuid");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(600000));

        accessCookie  = ResponseCookie.from("auth_token", "access-jwt").httpOnly(true).build();
        refreshCookie = ResponseCookie.from("refresh_token", "refresh-uuid").httpOnly(true).build();
        clearCookie   = ResponseCookie.from("auth_token", "").maxAge(0).build();
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithBothCookies() {
        LoginRequestDTO dto = new LoginRequestDTO("john", "Pass1234");
        when(userService.login(dto)).thenReturn(profile);
        when(userService.getRoleByUsername("john")).thenReturn(Role.USER);
        when(jwtUtil.generateToken("john", Role.USER)).thenReturn("access-jwt");
        when(jwtUtil.getExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(1)).thenReturn(refreshToken);
        when(cookieUtil.buildAccessTokenCookie("access-jwt", 900L)).thenReturn(accessCookie);
        when(cookieUtil.buildRefreshTokenCookie("refresh-uuid", 7 * 24 * 60 * 60))
                .thenReturn(refreshCookie);

        ResponseEntity<?> response = userController.login(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .contains(accessCookie.toString(), refreshCookie.toString());
        assertThat(response.getBody()).isEqualTo(profile);
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    void register_newUser_returns201WithProfile() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john", "Pass1234", "john@test.com");
        when(userService.saveUser(dto)).thenReturn(profile);

        ResponseEntity<?> response = userController.register(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    // ── refresh ─────────────────────────────────────────────────────────────────

    @Test
    void refreshToken_validRefreshCookie_returns200WithNewCookies() {
        HttpServletRequest request = mockRequestWithCookie("refresh_token", "refresh-uuid");
        when(refreshTokenService.findByToken("refresh-uuid")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtUtil.generateToken("john", Role.USER)).thenReturn("new-access-jwt");
        when(jwtUtil.getExpirationSeconds()).thenReturn(900L);

        RefreshToken rotated = new RefreshToken();
        rotated.setUser(user);
        rotated.setToken("new-refresh-uuid");
        rotated.setExpiryDate(Instant.now().plusSeconds(600000));
        when(refreshTokenService.rotateRefreshToken(refreshToken)).thenReturn(rotated);

        ResponseCookie newAccess  = ResponseCookie.from("auth_token", "new-access-jwt").httpOnly(true).build();
        ResponseCookie newRefresh = ResponseCookie.from("refresh_token", "new-refresh-uuid").httpOnly(true).build();
        when(cookieUtil.buildAccessTokenCookie("new-access-jwt", 900L)).thenReturn(newAccess);
        when(cookieUtil.buildRefreshTokenCookie("new-refresh-uuid", 7 * 24 * 60 * 60)).thenReturn(newRefresh);

        ResponseEntity<?> response = userController.refreshToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .contains(newAccess.toString(), newRefresh.toString());
    }

    @Test
    void refreshToken_missingRefreshCookie_returns401() {
        HttpServletRequest request = mockRequestWithNoCookies();

        ResponseEntity<?> response = userController.refreshToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── logout ──────────────────────────────────────────────────────────────────

    @Test
    void logout_withBothCookies_blacklistsAccessTokenAndDeletesRefreshToken() {
        HttpServletRequest request = mockRequestWithCookies(
                new Cookie("auth_token", "access-jwt"),
                new Cookie("refresh_token", "refresh-uuid"));
        when(jwtUtil.extractJti("access-jwt")).thenReturn("jti-123");
        when(jwtUtil.getRemainingValiditySeconds("access-jwt")).thenReturn(600L);
        when(cookieUtil.clearCookie("auth_token")).thenReturn(clearCookie);
        when(cookieUtil.clearCookie("refresh_token")).thenReturn(clearCookie);

        ResponseEntity<?> response = userController.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tokenBlacklistService).blacklist("jti-123", 600L);
        verify(refreshTokenService).deleteByToken("refresh-uuid");
    }

    @Test
    void logout_noCookies_returns204WithoutThrowingExceptions() {
        HttpServletRequest request = mockRequestWithNoCookies();
        when(cookieUtil.clearCookie("auth_token")).thenReturn(clearCookie);
        when(cookieUtil.clearCookie("refresh_token")).thenReturn(clearCookie);

        ResponseEntity<?> response = userController.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verifyNoInteractions(tokenBlacklistService, refreshTokenService);
    }

    // ── forgot-password ──────────────────────────────────────────────────────

    @Test
    void forgotPassword_validEmail_returns200WithMessage() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("john@test.com");

        ResponseEntity<?> response = userController.forgotPassword(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordResetService).requestPasswordReset("john@test.com");
    }

    // ── reset-password ────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_returns200WithMessage() {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("token-abc");
        dto.setNewPassword("NewPass1");

        ResponseEntity<?> response = userController.resetPassword(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordResetService).resetPassword("token-abc", "NewPass1");
    }

    // ── delete account ────────────────────────────────────────────────────────

    @Test
    void deleteAccount_correctPassword_returns204AndClearsCookies() {
        Principal principal = () -> "john";
        DeleteAccountRequestDTO dto = new DeleteAccountRequestDTO();
        dto.setPassword("Pass1234");
        HttpServletRequest request = mockRequestWithCookie("auth_token", "access-jwt");

        when(jwtUtil.extractJti("access-jwt")).thenReturn("jti-delete");
        when(jwtUtil.getRemainingValiditySeconds("access-jwt")).thenReturn(300L);
        when(cookieUtil.clearCookie("auth_token")).thenReturn(clearCookie);
        when(cookieUtil.clearCookie("refresh_token")).thenReturn(clearCookie);

        ResponseEntity<?> response = userController.deleteAccount(principal, dto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteAccount("john", "Pass1234");
        verify(tokenBlacklistService).blacklist("jti-delete", 300L);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpServletRequest mockRequestWithCookie(String name, String value) {
        return mockRequestWithCookies(new Cookie(name, value));
    }

    private HttpServletRequest mockRequestWithNoCookies() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(null);
        return req;
    }

    private HttpServletRequest mockRequestWithCookies(Cookie... cookies) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(cookies);
        return req;
    }
}
