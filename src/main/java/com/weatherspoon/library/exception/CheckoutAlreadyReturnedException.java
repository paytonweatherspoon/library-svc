package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a return is requested for a checkout that's already been closed out.
 * Maps to 409 Conflict.
 */
public class CheckoutAlreadyReturnedException extends LibraryException {

    public CheckoutAlreadyReturnedException(Long checkoutId, Instant timestamp) {
        super("Checkout %d has already been returned".formatted(checkoutId), HttpStatus.CONFLICT, timestamp);
    }
}
