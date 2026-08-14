package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a return is requested for a book that has no active checkout. Maps to
 * 400 Bad Request.
 */
public class BookNotCheckedOutException extends LibraryException {

    public BookNotCheckedOutException(Long bookId, Instant timestamp) {
        super("Book %d is currently checked-in. It should not be able to be returned".formatted(bookId),
                HttpStatus.BAD_REQUEST, timestamp);
    }

}
