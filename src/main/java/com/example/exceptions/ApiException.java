package com.example.exceptions;

import org.springframework.http.HttpStatus;

// The handler returns these messages to clients verbatim, so they must never carry internal details.
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
