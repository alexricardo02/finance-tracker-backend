package com.example.security;

import com.example.models.IdempotencyKey;
import com.example.repository.IdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    @InjectMocks private IdempotencyFilter filter;

    @Test
    void doFilter_nonMutationPath_skipsIdempotencyCheck() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/settings");
        when(request.getMethod()).thenReturn("PUT");

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(idempotencyKeyRepository);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_missingIdempotencyKeyHeader_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/incomes");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(idempotencyKeyRepository);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_newKey_reservesAndPersistsSuccessfulResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/incomes");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("key-123");
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_CREATED);
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
        when(idempotencyKeyRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.of(new IdempotencyKey()));

        filter.doFilterInternal(request, response, chain);

        verify(idempotencyKeyRepository).saveAndFlush(any(IdempotencyKey.class));
        verify(chain).doFilter(eq(request), any());
        verify(idempotencyKeyRepository).findByIdempotencyKey("key-123");
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void doFilter_failedResponse_releasesKeyInsteadOfCaching() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/incomes");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("key-failed");
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        IdempotencyKey reserved = new IdempotencyKey();
        when(idempotencyKeyRepository.findByIdempotencyKey("key-failed"))
                .thenReturn(Optional.of(reserved));

        filter.doFilterInternal(request, response, chain);

        verify(idempotencyKeyRepository).delete(reserved);
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    void doFilter_duplicateKey_stillProcessing_returns409_doesNotCallChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/incomes");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("key-inflight");
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        IdempotencyKey inflight = new IdempotencyKey();
        inflight.setResponseStatus(0);
        when(idempotencyKeyRepository.findByIdempotencyKey("key-inflight")).thenReturn(Optional.of(inflight));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_duplicateKey_alreadyCompleted_replaysCachedResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/incomes");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Idempotency-Key")).thenReturn("key-done");
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        IdempotencyKey completed = new IdempotencyKey();
        completed.setResponseStatus(201);
        completed.setResponseBody("{\"id\":1}");
        when(idempotencyKeyRepository.findByIdempotencyKey("key-done")).thenReturn(Optional.of(completed));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(201);
        verify(chain, never()).doFilter(any(), any());
        assertThat(sw.toString()).contains("\"id\":1");
    }
}