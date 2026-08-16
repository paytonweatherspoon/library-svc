package com.weatherspoon.library.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /api/v1/checkouts}: identifies which user is checking
 * out which book.
 *
 * @param userId the acting user's ID; must be present and positive
 * @param bookId the book's ID; must be present and positive
 */
public record LibraryRequest (
    @NotNull @Positive  Long userId,
    @NotNull @Positive  Long bookId
) {
}
