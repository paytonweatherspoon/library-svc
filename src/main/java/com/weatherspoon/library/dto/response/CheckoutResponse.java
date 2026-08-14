package com.weatherspoon.library.dto.response;

import lombok.Builder;

import java.time.Instant;

/**
 * Response returned after a successful book checkout.
 *
 * @param userName     the checking-out user's full name
 * @param bookName     the checked-out book's title
 * @param checkoutTime when the checkout occurred
 * @param dueTime      when the book is due back
 */
@Builder
public record CheckoutResponse(
        String userName,
        String bookName,
        Instant checkoutTime,
        Instant dueTime
) { }
