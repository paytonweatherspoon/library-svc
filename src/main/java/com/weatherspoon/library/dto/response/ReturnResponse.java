package com.weatherspoon.library.dto.response;

import lombok.Builder;

/**
 * Response returned after a successful book return.
 *
 * @param returningUser the full name of the user who made the return request
 * @param bookReturned  the returned book's title
 * @param returnTime    when the return occurred
 */
@Builder
public record ReturnResponse(
        String returningUser,
        String bookReturned,
        String returnTime
) {
}
