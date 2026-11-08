package com.example.exceptions;

// Extends SecurityException so callers that treat ownership failures as security failures keep working;
// the handler maps this subtype to 403, while plain SecurityException stays 401 for bad credentials.
public class ForbiddenResourceException extends SecurityException {

    public ForbiddenResourceException(String message) {
        super(message);
    }
}
