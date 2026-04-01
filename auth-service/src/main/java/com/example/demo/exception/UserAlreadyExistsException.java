package com.example.demo.exception;

// Specific exceptions
public class UserAlreadyExistsException extends KeycloakException {
    public UserAlreadyExistsException(String message) {
        super(message, 409);
    }
}
