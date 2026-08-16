package com.weatherspoon.library.service;

import com.weatherspoon.library.dto.BookDetails;
import com.weatherspoon.library.dto.CheckoutDetails;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.request.ReturnCheckoutRequest;
import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.entity.Book;
import com.weatherspoon.library.entity.Checkout;
import com.weatherspoon.library.entity.User;
import com.weatherspoon.library.exception.BookNotFoundException;
import com.weatherspoon.library.exception.BookUnavailableException;
import com.weatherspoon.library.exception.CheckoutAlreadyReturnedException;
import com.weatherspoon.library.exception.CheckoutNotFoundException;
import com.weatherspoon.library.exception.UserNotFoundException;
import com.weatherspoon.library.repository.BookRepository;
import com.weatherspoon.library.repository.CheckoutRepository;
import com.weatherspoon.library.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Core business logic for checking out and returning books, and for looking up what a
 * user currently has checked out. Each write operation is transactional so the book's
 * availability flag and its {@link Checkout} record are always updated together.
 */
@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);

    private final CheckoutRepository checkoutRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;

    /**
     * Returns a checked-out book: marks it available again and closes out the given checkout.
     * @param checkoutId the checkout to close
     * @param request api request for returning book
     * @return ReturnResponse for book return
     * @throws CheckoutNotFoundException if no checkout exists with that ID
     * @throws CheckoutAlreadyReturnedException if the checkout has already been closed
     * @throws UserNotFoundException if the returning user doesn't exist
     */
    @Transactional
    public ReturnResponse returnBook(Long checkoutId, ReturnCheckoutRequest request) {

        Long returnedByUserId = request.returnedByUserId();

        Checkout bookRental = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new CheckoutNotFoundException(checkoutId, Instant.now()));

        if (!bookRental.isActive()) {
            throw new CheckoutAlreadyReturnedException(checkoutId, Instant.now());
        }

        User user = userRepo.findById(returnedByUserId)
                .orElseThrow(() -> new UserNotFoundException(returnedByUserId, Instant.now()));

        // Still accept the return even if it's not the original borrower, but flag it.
        if (!bookRental.getUser().getUserId().equals(returnedByUserId)) {
            logger.warn("Book {} with id {} was returned by another user", bookRental.getBook().getTitle(),
                    bookRental.getBook().getBookId());
        }
        bookRental.getBook().setIsAvailable(true);
        bookRental.setReturnTime(Instant.now());
        checkoutRepo.save(bookRental);
        return ReturnResponse.builder()
                .checkoutId(bookRental.getCheckoutId())
                .bookReturned(bookRental.getBook().getTitle())
                .returningUser("%s %s".formatted(user.getFirstName(), user.getLastName()))
                .returnTime(bookRental.getReturnTime().toString())
                .build();
    }

    /**
     * Checks out a book for a user.
     * Fails when the user and/or book do not exist. Marks the book unavailable and creates a checkout.
     * @param checkoutRequest api request to checkout book
     * @return checkout response
     * @throws BookNotFoundException if the book doesn't exist
     * @throws UserNotFoundException if the user doesn't exist
     * @throws BookUnavailableException if the book is already checked out
     */
    @Transactional
    public CheckoutResponse checkoutBook(LibraryRequest checkoutRequest) {

        Long bookId = checkoutRequest.bookId();
        Long userId = checkoutRequest.userId();

        Book book = bookRepo.findById(checkoutRequest.bookId())
                .orElseThrow(() -> new BookNotFoundException(bookId, Instant.now()));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId, Instant.now()));

        if (!book.getIsAvailable()) {
            throw new BookUnavailableException(bookId, Instant.now());
        }

        book.setIsAvailable(false);
        Checkout checkout = new Checkout(user, book);
        checkoutRepo.save(checkout);
        return CheckoutResponse.builder()
                .checkoutId(checkout.getCheckoutId())
                .bookName(book.getTitle())
                .checkoutTime(checkout.getCheckoutTime())
                .dueTime(checkout.getDueTime())
                .userName("%s %s".formatted(user.getFirstName(), user.getLastName()))
                .build();
    }

    /**
     * Fetches a single checkout by ID, active or already returned.
     * @param checkoutId the checkout to look up
     * @return the checkout's full details
     * @throws CheckoutNotFoundException if no checkout exists with that ID
     */
    @Transactional
    public CheckoutDetails getCheckout(Long checkoutId) {
        Checkout checkout = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new CheckoutNotFoundException(checkoutId, Instant.now()));
        User user = checkout.getUser();
        return CheckoutDetails.builder()
                .checkoutId(checkout.getCheckoutId())
                .userName("%s %s".formatted(user.getFirstName(), user.getLastName()))
                .bookName(checkout.getBook().getTitle())
                .checkoutTime(checkout.getCheckoutTime())
                .dueTime(checkout.getDueTime())
                .returnTime(checkout.getReturnTime())
                .build();
    }

    /**
     * Returns the books a user currently has checked out, with checkout and due dates.
     * @param userId user identifier
     * @return list of books with due date and original date of checkout
     * @throws UserNotFoundException if the user doesn't exist
     */
    @Transactional
    public UserActiveCheckout getBooksCheckedOutByUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId, Instant.now()));
        List<BookDetails> activeBooks = checkoutRepo.getActiveCheckoutsByUser(userId).stream().map(
        checkout -> {
            Book book = checkout.getBook();
            return new BookDetails(checkout.getCheckoutId(), book.getTitle(), checkout.getCheckoutTime(),
                    checkout.getDueTime());
        }).toList();
        return new UserActiveCheckout("%s %s".formatted(user.getFirstName(), user.getLastName()), activeBooks);
    }

}
