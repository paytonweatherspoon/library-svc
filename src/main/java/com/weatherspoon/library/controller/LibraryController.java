package com.weatherspoon.library.controller;


import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for checking out and returning library books. Delegates all business logic
 * to {@link LibraryService}; domain errors thrown from there are translated to HTTP
 * responses by {@link com.weatherspoon.library.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    /**
     * Checks out a book for a user, provided the book exists, the user exists, and the
     * book is currently available.
     *
     * @param request the user/book pair to check out
     * @return 201 with the checkout details (who, what, when due)
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkoutBook(@Valid @RequestBody LibraryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.checkoutBook(request));
    }

    /**
     * Returns a previously checked-out book, marking it available again.
     *
     * @param request the user/book pair to return
     * @return 200 with the return details
     */
    @PostMapping("/return")
    public ResponseEntity<ReturnResponse> returnBook(@Valid @RequestBody LibraryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(libraryService.returnBook(request));
    }

    /**
     * Lists the books a user currently has checked out.
     *
     * @param userId the user to look up
     * @return 200 with the user's active checkouts (empty list if none)
     */
    @GetMapping("/userActiveCheckouts")
    public ResponseEntity<UserActiveCheckout> getBooksCheckedOutByUser(@RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(libraryService.getBooksCheckedOutByUser(userId));
    }

}
