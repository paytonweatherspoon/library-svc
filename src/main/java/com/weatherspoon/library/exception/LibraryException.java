package com.weatherspoon.library.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Base type for all domain-specific errors raised by the library service. Carries the
 * HTTP status it should map to, so {@link com.weatherspoon.library.GlobalExceptionHandler}
 * can translate any subclass to a response without a switch statement per exception type.
 */
@Getter
public abstract class LibraryException extends RuntimeException {

    private final HttpStatus status;
    private final Instant exceptionTimestamp;

    /**
     * @param errorMessage message surfaced to the API caller
     * @param status       HTTP status this error should map to
     * @param timestamp    when the error occurred
     */
    public LibraryException(String errorMessage, HttpStatus status, Instant timestamp) {
        super(errorMessage);
        this.status = status;
        this.exceptionTimestamp = timestamp;
    }
}

