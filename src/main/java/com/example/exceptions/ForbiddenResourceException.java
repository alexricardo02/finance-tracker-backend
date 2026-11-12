package com.example.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenResourceException extends ApiException {

    public ForbiddenResourceException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
