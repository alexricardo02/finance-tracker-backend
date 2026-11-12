package com.example.exceptions;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

// Clients see ErrorResponse.message as-is. Only ApiException messages and validation messages we
// declared are passed through; framework and library messages are replaced by a generic one per status.
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    private static final String INVALID_REQUEST = "Invalid request";

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.debug("Request rejected with {}: {}", ex.getStatus().value(), ex.getMessage());
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument while handling request", ex);
        return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
    }

    // Our code throws ResourceNotFoundException, so this one comes from JPA and names entity classes and ids.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("JPA entity not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Resource not found");
    }

    // Raised by @Validated controllers through method validation AOP, not by Spring MVC itself.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing((ConstraintViolation<?> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(INVALID_REQUEST);
        log.warn("Request rejected with 400: constraint violation");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    // Method security fails inside the controller, after ExceptionTranslationFilter has already passed,
    // so the 401/403 split of the filter chain is reproduced here.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            log.warn("Request rejected with 401: unauthenticated access to a secured method");
            return build(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        log.warn("Request rejected with 403: access denied");
        return build(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Request rejected with 401: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "The operation violates a data constraint (invalid or duplicate reference)");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Sorted because the validator reports violations in no particular order.
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(e -> Objects.toString(e.getDefaultMessage(), "")))
                .map(GlobalExceptionHandler::fieldErrorMessage)
                .findFirst()
                .or(() -> ex.getBindingResult().getGlobalErrors().stream()
                        .map(ObjectError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .sorted()
                        .findFirst())
                .orElse(INVALID_REQUEST);
        return respond(ex, headers, status, request, message);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> new Violation(
                        result.getMethodParameter().getParameterIndex(),
                        Optional.ofNullable(result.getContainerIndex()).orElse(-1),
                        error instanceof FieldError fieldError ? fieldError.getField() : "",
                        resolvableMessage(error))))
                .sorted(Comparator.comparingInt(Violation::parameter)
                        .thenComparingInt(Violation::index)
                        .thenComparing(Violation::field)
                        .thenComparing(Violation::message))
                .map(Violation::message)
                .findFirst()
                .orElse(INVALID_REQUEST);
        return respond(ex, headers, status, request, message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = invalidJsonField(ex);
        String message = field != null ? "Invalid value for field '" + field + "'" : "Malformed JSON request";
        return respond(ex, headers, status, request, message);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        String message = name != null ? "Invalid value for parameter '" + name + "'" : INVALID_REQUEST;
        return respond(ex, headers, status, request, message);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, "Missing required parameter '" + ex.getParameterName() + "'");
    }

    // Every other framework exception the base class handles ends here with Spring's status and headers.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return respond(ex, headers, statusCode, request, genericMessage(statusCode));
    }

    private ResponseEntity<Object> respond(Exception ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request, String message) {
        if (request instanceof ServletWebRequest servletRequest
                && servletRequest.getResponse() != null && servletRequest.getResponse().isCommitted()) {
            return null;
        }
        if (status.is5xxServerError()) {
            log.error("Request failed with {}", status.value(), ex);
            // Lets the observation filter record the exception, as the base class does.
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, RequestAttributes.SCOPE_REQUEST);
        } else {
            // Framework messages can quote the request body, so only the class name is logged.
            log.warn("Request rejected with {} ({}): {}", status.value(), ex.getClass().getSimpleName(), message);
        }
        return ResponseEntity.status(status).headers(headers)
                .body(new ErrorResponse(status.value(), message, LocalDateTime.now()));
    }

    private static String genericMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Malformed or invalid request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 405 -> "Method not allowed";
            case 406, 415 -> "Unsupported media type";
            case 413 -> "Payload too large";
            case 503 -> "Service temporarily unavailable";
            default -> status.is4xxClientError() ? "Request could not be processed" : UNEXPECTED_ERROR;
        };
    }

    // A binding failure carries the converter's message, which names Java types.
    private static String fieldErrorMessage(FieldError error) {
        if (error.isBindingFailure() || error.getDefaultMessage() == null) {
            return "Invalid value for field '" + error.getField() + "'";
        }
        return error.getDefaultMessage();
    }

    private static String resolvableMessage(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : INVALID_REQUEST;
    }

    // Field names only: Jackson's own message contains the rejected value and target class names.
    @Nullable
    private static String invalidJsonField(HttpMessageNotReadableException ex) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch) {
                StringBuilder path = new StringBuilder();
                for (JsonMappingException.Reference reference : mismatch.getPath()) {
                    if (reference.getFieldName() != null) {
                        path.append(path.isEmpty() ? "" : ".").append(reference.getFieldName());
                    } else if (reference.getIndex() >= 0) {
                        path.append('[').append(reference.getIndex()).append(']');
                    }
                }
                return path.isEmpty() ? null : path.toString();
            }
        }
        return null;
    }

    private record Violation(int parameter, int index, String field, String message) {
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, LocalDateTime.now()));
    }
}
