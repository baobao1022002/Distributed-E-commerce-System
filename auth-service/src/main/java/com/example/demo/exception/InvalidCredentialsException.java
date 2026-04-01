package com.example.demo.exception;

public class InvalidCredentialsException extends KeycloakException {
    public InvalidCredentialsException(String message) {
        super(message, 401);
    }
}

