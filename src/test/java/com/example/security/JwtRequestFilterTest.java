package com.example.security;

import com.example.models.User;
import com.example.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    @InjectMocks private JwtRequestFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noCookieOrHeader_continuesUnauthenticated() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_validCookieToken_activeUser_setsAuthentication() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth_token", "valid.jwt.token")});
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("john");
        when(jwtUtil.extractJti("valid.jwt.token")).thenReturn("jti-1");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);

        User user = new User();
        user.setActive(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtUtil.extractRole("valid.jwt.token")).thenReturn("USER");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("john");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_blacklistedToken_doesNotAuthenticate_stillContinuesChain() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth_token", "revoked.jwt.token")});
        when(jwtUtil.extractUsername("revoked.jwt.token")).thenReturn("john");
        when(jwtUtil.extractJti("revoked.jwt.token")).thenReturn("jti-2");
        when(jwtUtil.isTokenValid("revoked.jwt.token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("jti-2")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userRepository, never()).findByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_deactivatedUser_returns401_doesNotContinueChain() throws Exception {
        // WHY: @SQLRestriction("is_active = true") means a deactivated/deleted user
        // won't be found by findByUsername — the filter must treat that as unauthorized.
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth_token", "valid.jwt.token")});
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("john");
        when(jwtUtil.extractJti("valid.jwt.token")).thenReturn("jti-1");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("jti-1")).thenReturn(false);
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_bearerHeaderFallback_usedWhenNoCookiePresent() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer header.jwt.token");
        when(jwtUtil.extractUsername("header.jwt.token")).thenReturn("john");
        when(jwtUtil.extractJti("header.jwt.token")).thenReturn("jti-3");
        when(jwtUtil.isTokenValid("header.jwt.token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("jti-3")).thenReturn(false);

        User user = new User();
        user.setActive(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtUtil.extractRole("header.jwt.token")).thenReturn("USER");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilter_invalidToken_doesNotAuthenticate() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth_token", "expired.jwt.token")});
        when(jwtUtil.extractUsername("expired.jwt.token")).thenReturn("john");
        when(jwtUtil.extractJti("expired.jwt.token")).thenReturn("jti-4");
        when(jwtUtil.isTokenValid("expired.jwt.token")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}