package com.example.security;

import com.example.models.IdempotencyKey;
import com.example.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

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

        // Reserva atómica: status=0 significa "procesando". El constraint UNIQUE
        // de idempotencyKey en DB es lo que realmente previene la doble ejecución
        // ante requests concurrentes (antes solo se validaba con un SELECT previo).
        IdempotencyKey reservation = new IdempotencyKey();
        reservation.setIdempotencyKey(idempotencyKey);
        reservation.setResponseStatus(0);
        reservation.setResponseBody("");
        reservation.setUserId(0);

        try {
            idempotencyKeyRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            IdempotencyKey existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

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

            idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(rec -> {
                rec.setResponseStatus(wrappedResponse.getStatus());
                rec.setResponseBody(body);
                idempotencyKeyRepository.save(rec);
            });
        } else {
            // Si falló, liberamos la key para permitir reintento con la misma Idempotency-Key
            idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                    .ifPresent(idempotencyKeyRepository::delete);
        }

        wrappedResponse.copyBodyToResponse();
    }
}