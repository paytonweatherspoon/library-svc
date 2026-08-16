package com.weatherspoon.library.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Full representation of a checkout resource, returned by {@code GET /api/v1/checkouts/{checkoutId}}.
 *
 * @param checkoutId   the checkout's ID
 * @param userName     the checking-out user's full name
 * @param bookName     the checked-out book's title
 * @param checkoutTime when the checkout occurred
 * @param dueTime      when the book is due back
 * @param returnTime   when the book was returned; {@code null} if the checkout is still active
 */
@Builder
public record CheckoutDetails(
        Long checkoutId,
        String userName,
        String bookName,
        Instant checkoutTime,
        Instant dueTime,
        Instant returnTime
) {
}
