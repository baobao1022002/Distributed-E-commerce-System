package com.example.demo.exception;

public class UserCreationFailedException extends KeycloakException {
    public UserCreationFailedException(String message, int statusCode) {
        super(message, statusCode);
    }
}
