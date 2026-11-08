package com.example.security;

import com.example.models.IdempotencyKey;
import com.example.repository.IdempotencyKeyRepository;
import com.example.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        boolean appliesToPath = request.getRequestURI().startsWith("/api/incomes")
                || request.getRequestURI().startsWith("/api/expenses");

        String method = request.getMethod();
        boolean isProtectedMethod = method.equals("POST") || method.equals("PUT") || method.equals("DELETE");

        if (!isProtectedMethod || !appliesToPath) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Integer userId = authenticatedUserId();
        // Anonymous requests continue so authorization can reject them; they must never reach a stored response.
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        // Atomic reservation: status=0 means "processing". The UNIQUE constraint
        // on (userId, idempotencyKey) in the database is what actually prevents duplicate execution
        // under concurrent requests.
        IdempotencyKey reservation = new IdempotencyKey();
        reservation.setIdempotencyKey(idempotencyKey);
        reservation.setResponseStatus(0);
        reservation.setResponseBody("");
        reservation.setUserId(userId);

        try {
            idempotencyKeyRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            IdempotencyKey existing = idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);

            if (existing == null || existing.getResponseStatus() == 0) {
                response.setStatus(HttpStatus.CONFLICT.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Duplicate request already in progress\"}");
                return;
            }

            response.setStatus(existing.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(existing.getResponseBody());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        if (wrappedResponse.getStatus() >= 200 && wrappedResponse.getStatus() < 300) {
            byte[] bodyBytes = wrappedResponse.getContentAsByteArray();
            String body = new String(bodyBytes, response.getCharacterEncoding() != null
                    ? response.getCharacterEncoding() : "UTF-8");

            idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).ifPresent(rec -> {
                rec.setResponseStatus(wrappedResponse.getStatus());
                rec.setResponseBody(body);
                idempotencyKeyRepository.save(rec);
            });
        } else {
            // If it failed, release the key to allow a retry with the same Idempotency-Key
            idempotencyKeyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .ifPresent(idempotencyKeyRepository::delete);
        }

        wrappedResponse.copyBodyToResponse();
    }

    private Integer authenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).map(user -> user.getUserId()).orElse(null);
    }
}