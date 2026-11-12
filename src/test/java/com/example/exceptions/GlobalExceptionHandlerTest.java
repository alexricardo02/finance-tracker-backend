package com.example.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404_genericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFound(
                new EntityNotFoundException("Unable to find com.example.models.Income with id 42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
    }

    @Test
    void handleIllegalArgument_returns400_genericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid data"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request");
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
    void handleMethodArgumentNotValid_returns400_withFirstFieldErrorByFieldName() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("dto", "date", "The date is required"),
                new FieldError("dto", "amount", "Amount is required")));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ErrorResponse) response.getBody()).getMessage()).isEqualTo("Amount is required");
    }
}
