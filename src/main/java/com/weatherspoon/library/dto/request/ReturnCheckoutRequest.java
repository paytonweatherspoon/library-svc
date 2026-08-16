package com.weatherspoon.library.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for closing out a checkout via {@code POST /api/v1/checkouts/{checkoutId}/return}.
 *
 * @param returnedByUserId the user performing the return; must be present and positive
 */
public record ReturnCheckoutRequest(
        @NotNull @Positive Long returnedByUserId
) {
}
