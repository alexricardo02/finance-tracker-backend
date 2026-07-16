package com.example.security;

import jakarta.servlet.FilterChain;
import com.example.repository.UserRepository;
import com.example.models.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jwt = extractFromCookie(request);
        if (jwt == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        String username = null;
        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("No se pudo extraer el username del token");
            }
        }
        
        

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jti = jwtUtil.extractJti(jwt);
            if (jwtUtil.isTokenValid(jwt) && !tokenBlacklistService.isBlacklisted(jti)) {
            	
            	boolean isActive = userRepository.findByUsername(username)
                        .map(User::isActive)
                        .orElse(false);
            	
            	if (!isActive) {
                    // Si el usuario no existe (o is_active=false por el @Where), bloqueamos la petición
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o cuenta desactivada");
                    return;
                }
            	
                String role = jwtUtil.extractRole(jwt);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }

    private String extractFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("auth_token".equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
