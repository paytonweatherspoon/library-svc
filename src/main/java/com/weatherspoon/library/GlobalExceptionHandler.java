package com.weatherspoon.library;

import com.weatherspoon.library.dto.response.ExceptionResponse;
import com.weatherspoon.library.exception.LibraryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;


/**
 * Translates exceptions raised anywhere in the request-handling chain into
 * {@link ExceptionResponse} bodies with the appropriate HTTP status, so controllers
 * don't need their own try/catch blocks.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all domain-specific errors ({@link com.weatherspoon.library.exception.BookNotFoundException},
     * {@link com.weatherspoon.library.exception.UserNotFoundException}, etc.), using the
     * HTTP status each exception carries.
     */
    @ExceptionHandler(LibraryException.class)
    public ResponseEntity<ExceptionResponse> handleLibraryExceptions(LibraryException ex) {
        return ResponseEntity.status(ex.getStatus()).body(buildExceptionResponse(ex));
    }

    /**
     * Handles {@code @Valid} request-body validation failures (e.g. a missing or
     * non-positive {@code userId}/{@code bookId}). Maps to 422 Unprocessable Content.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleInvalidRequestValues(MethodArgumentNotValidException ex) {
        logger.error(ex.getMessage());
        ExceptionResponse resp = ExceptionResponse.builder()
                .message("Invalid request parameters")
                .timestamp(Instant.now())
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(resp);
    }

    /**
     * Handles a book's {@code @Version} having changed between read and write —
     * i.e. two concurrent checkout/return requests raced for the same book. Maps to
     * 409 Conflict; the caller is expected to retry.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ExceptionResponse> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking conflict: {}", ex.getMessage());
        ExceptionResponse resp = ExceptionResponse.builder()
                .message("The book was modified by another request. Please retry.")
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
    }

    private ExceptionResponse buildExceptionResponse(LibraryException ex) {
        return ExceptionResponse.builder()
                .message(ex.getMessage())
                .status(ex.getStatus())
                .timestamp(ex.getExceptionTimestamp())
                .build();
    }
}
