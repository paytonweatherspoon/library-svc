package com.weatherspoon.library.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown when a request references a checkout ID that doesn't exist. Maps to 404 Not Found.
 */
public class CheckoutNotFoundException extends LibraryException {

    public CheckoutNotFoundException(Long checkoutId, Instant timestamp) {
        super("Checkout %d does not exist".formatted(checkoutId), HttpStatus.NOT_FOUND, timestamp);
    }
}
