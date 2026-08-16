package com.weatherspoon.library.dto.response;

import lombok.Builder;

/**
 * Response returned after a successful book return.
 *
 * @param checkoutId    the closed checkout's ID
 * @param returningUser the full name of the user who made the return request
 * @param bookReturned  the returned book's title
 * @param returnTime    when the return occurred
 */
@Builder
public record ReturnResponse(
        Long checkoutId,
        String returningUser,
        String bookReturned,
        String returnTime
) {
}
