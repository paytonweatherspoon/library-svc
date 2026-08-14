package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a request references a book ID that doesn't exist. Maps to 404 Not Found.
 */
public class BookNotFoundException extends LibraryException {

    public BookNotFoundException(Long bookId, Instant timestamp) {
        super("Book %d does not exist".formatted(bookId), HttpStatus.NOT_FOUND, timestamp);
    }
}
