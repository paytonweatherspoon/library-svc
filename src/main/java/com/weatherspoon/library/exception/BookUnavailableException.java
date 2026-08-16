package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a checkout is requested for a book that's already checked out to someone
 * else. Maps to 409 Conflict.
 */
public class BookUnavailableException extends LibraryException {

    public BookUnavailableException(Long bookId, Instant timestamp) {
        super("Book %d is currently checked out to another user".formatted(bookId), HttpStatus.CONFLICT, timestamp);
    }
}
