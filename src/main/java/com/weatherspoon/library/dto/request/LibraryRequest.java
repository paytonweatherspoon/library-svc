package com.weatherspoon.library.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body shared by the checkout and return endpoints: identifies which user is
 * acting on which book.
 *
 * @param userId the acting user's ID; must be present and positive
 * @param bookId the book's ID; must be present and positive
 */
public record LibraryRequest (
    @NotNull @Positive  Long userId,
    @NotNull @Positive  Long bookId
) {
    public LibraryRequest(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }
}
