package com.weatherspoon.library.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * A single book within a user's list of active checkouts.
 *
 * @param checkoutId   the checkout's ID (needed to call {@code POST /checkouts/{checkoutId}/return})
 * @param title        the book's title
 * @param checkoutTime when the book was checked out
 * @param dueDate      when the book is due back
 */
@Builder
public record BookDetails(
        Long checkoutId,
        String title,
        Instant checkoutTime,
        Instant dueDate
) {
}
