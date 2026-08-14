package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a request references a user ID that doesn't exist. Maps to 404 Not Found.
 */
public class UserNotFoundException extends LibraryException {

    public UserNotFoundException(Long userId, Instant timestamp) {
        super("User not found with id: " + userId, HttpStatus.NOT_FOUND, timestamp);
    }
}
