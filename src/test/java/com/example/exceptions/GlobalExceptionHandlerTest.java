package com.example.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFound(
                new EntityNotFoundException("Income not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Income not found");
    }

    @Test
    void handleIllegalArgument_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid data"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleSecurityException_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleSecurityException(
                new SecurityException("Not allowed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleDataIntegrity_returns409_genericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("constraint violation"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage())
                .isEqualTo("The operation violates a data constraint (invalid or duplicate reference)");
    }

    @Test
    void handleResponseStatusException_returnsMatchingStatusAndReason() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Category not found");
    }

    @Test
    void handleRuntimeException_returns500_doesNotLeakInternalDetail() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(
                new RuntimeException("some internal detail that should not leak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("internal detail");
    }

    @Test
    void handleException_returns500_genericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleException(
                new Exception("leaky stack trace detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleValidationExceptions_returns400_withFirstFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "amount", "Amount is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Amount is required");
    }
}