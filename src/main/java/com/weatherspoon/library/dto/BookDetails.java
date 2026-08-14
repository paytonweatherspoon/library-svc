package com.weatherspoon.library.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * A single book within a user's list of active checkouts.
 *
 * @param title        the book's title
 * @param checkoutTime when the book was checked out
 * @param dueDate      when the book is due back
 */
@Builder
public record BookDetails(
        String title,
        Instant checkoutTime,
        Instant dueDate
) {
}
