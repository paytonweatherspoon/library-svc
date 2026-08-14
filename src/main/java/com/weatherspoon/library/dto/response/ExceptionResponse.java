package com.weatherspoon.library.dto.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard error body returned by {@link com.weatherspoon.library.GlobalExceptionHandler}
 * for any failed request.
 *
 * @param message   a human-readable description of what went wrong
 * @param status    the HTTP status the error was mapped to
 * @param timestamp when the error occurred
 */
@Builder
public record ExceptionResponse (
        String message,
        HttpStatus status,
        Instant timestamp
) { }
