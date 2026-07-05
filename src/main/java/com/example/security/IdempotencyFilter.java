package com.example.security;

import com.example.models.IdempotencyKey;
import com.example.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

        // WHY: only guard mutation endpoints where duplicates are dangerous
        boolean appliesToPath = request.getRequestURI().startsWith("/api/incomes")
                || request.getRequestURI().startsWith("/api/expenses");

        if (!request.getMethod().equals("POST") || !appliesToPath) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");

        // WHY: if the client doesn't send the header, behave as before (backwards compatible)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        var existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey saved = existing.get();
            response.setStatus(saved.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(saved.getResponseBody());
            return;
        }

        // WHY: wrap response to capture the body after the controller executes
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        // Only persist successful creations (2xx) to allow retry on failures
        if (wrappedResponse.getStatus() >= 200 && wrappedResponse.getStatus() < 300) {
            String username = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : null;

            byte[] bodyBytes = wrappedResponse.getContentAsByteArray();
            String body = new String(bodyBytes, response.getCharacterEncoding() != null
                    ? response.getCharacterEncoding() : "UTF-8");

            IdempotencyKey record = new IdempotencyKey();
            record.setIdempotencyKey(idempotencyKey);
            record.setResponseStatus(wrappedResponse.getStatus());
            record.setResponseBody(body);
            // userId is optional metadata here; 0 as placeholder if not resolvable
            record.setUserId(0);
            idempotencyKeyRepository.save(record);
        }

        wrappedResponse.copyBodyToResponse();
    }
}