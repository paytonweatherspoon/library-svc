package com.weatherspoon.library.dto.response;

import com.weatherspoon.library.dto.BookDetails;
import lombok.Builder;

import java.util.List;

/**
 * A user's current set of checked-out books.
 *
 * @param userName the user's full name
 * @param books    the books currently checked out by the user; empty if none
 */
@Builder
public record UserActiveCheckout(
        String userName,
        List<BookDetails> books
) {
}
