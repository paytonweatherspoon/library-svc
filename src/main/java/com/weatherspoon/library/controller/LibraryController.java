package com.weatherspoon.library.controller;


import com.weatherspoon.library.dto.CheckoutDetails;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.request.ReturnCheckoutRequest;
import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST API for the library's checkout resource: creating checkouts, closing them out
 * (returning a book), and listing a user's active checkouts. Delegates all business
 * logic to {@link LibraryService}; domain errors thrown from there are translated to
 * HTTP responses by {@link com.weatherspoon.library.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    /**
     * Creates a checkout (checks out a book for a user), provided the book exists, the
     * user exists, and the book is currently available.
     *
     * @param request the user/book pair to check out
     * @return 201, with a {@code Location} header for the new checkout and the checkout details
     */
    @PostMapping("/checkouts")
    public ResponseEntity<CheckoutResponse> checkoutBook(@Valid @RequestBody LibraryRequest request) {
        CheckoutResponse response = libraryService.checkoutBook(request);
        return ResponseEntity.created(URI.create("/api/v1/checkouts/" + response.checkoutId())).body(response);
    }

    /**
     * Returns a previously checked-out book, closing out the given checkout and marking
     * the book available again.
     *
     * @param checkoutId the checkout to close
     * @param request    who is performing the return
     * @return 200 with the return details
     */
    @PostMapping("/checkouts/{checkoutId}/return")
    public ResponseEntity<ReturnResponse> returnBook(@PathVariable Long checkoutId,
                                                      @Valid @RequestBody ReturnCheckoutRequest request) {
        return ResponseEntity.ok(libraryService.returnBook(checkoutId, request));
    }

    /**
     * Fetches a single checkout — the resource the {@code Location} header from a
     * successful {@code POST /checkouts} points to. Works for both active and
     * already-returned checkouts.
     *
     * @param checkoutId the checkout to look up
     * @return 200 with the checkout's full details
     */
    @GetMapping("/checkouts/{checkoutId}")
    public ResponseEntity<CheckoutDetails> getCheckout(@PathVariable Long checkoutId) {
        return ResponseEntity.ok(libraryService.getCheckout(checkoutId));
    }

    /**
     * Lists the books a user currently has checked out.
     *
     * @param userId the user to look up
     * @return 200 with the user's active checkouts (empty list if none)
     */
    @GetMapping("/users/{userId}/checkouts")
    public ResponseEntity<UserActiveCheckout> getBooksCheckedOutByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(libraryService.getBooksCheckedOutByUser(userId));
    }

}
