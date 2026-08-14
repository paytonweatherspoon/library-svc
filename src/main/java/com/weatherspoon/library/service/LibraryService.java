package com.weatherspoon.library.service;

import com.weatherspoon.library.dto.BookDetails;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.entity.Book;
import com.weatherspoon.library.entity.Checkout;
import com.weatherspoon.library.entity.User;
import com.weatherspoon.library.exception.BookNotCheckedOutException;
import com.weatherspoon.library.exception.BookNotFoundException;
import com.weatherspoon.library.exception.BookUnavailableException;
import com.weatherspoon.library.exception.UserNotFoundException;
import com.weatherspoon.library.repository.BookRepository;
import com.weatherspoon.library.repository.CheckoutRepository;
import com.weatherspoon.library.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class LibraryService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);

    private final CheckoutRepository checkoutRepo;
    private final BookRepository bookRepo;
    private final UserRepository userRepo;

    /**
     * Returning book and updating users' records to prevent late return.
     * @param request api request for returning book
     * @return ReturnResponse for book return
     * @throws BookNotFoundException if the book doesn't exist
     * @throws UserNotFoundException if the user doesn't exist
     * @throws BookNotCheckedOutException if the book has no active checkout
     */
    @Transactional
    public ReturnResponse returnBook(LibraryRequest request) {

        Long bookId = request.bookId();
        Long userId = request.userId();

        // first verify the user and book are valid
        Book book = bookRepo.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId, Instant.now()));
        User user = userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId, Instant.now()));

        // Get checkout info
        Checkout bookRental = checkoutRepo.getActiveCheckoutByBook(bookId)
                .orElseThrow(() -> new BookNotCheckedOutException(bookId, Instant.now()));

        // I believe we still want to take the book but should log out someone else returned it
        if (!bookRental.getUser().getUserId().equals(userId)) {
            logger.warn("Book {} with id {} was returned by another user", bookRental.getBook().getTitle(),
                    bookRental.getBook().getBookId());
        }
        bookRental.getBook().setIsAvailable(true);
        bookRental.setReturnTime(Instant.now());
        checkoutRepo.save(bookRental);
        return ReturnResponse.builder().bookReturned(book.getTitle())
                .returningUser("%s %s".formatted(user.getFirstName(), user.getLastName()))
                .returnTime(bookRental.getReturnTime().toString())
                .build();
    }

    /**
     * Checkout a book for a user.
     * Fails when a user and/or book do not exist. Marks a book as unavailable and creates a checkout.
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
                .bookName(book.getTitle())
                .checkoutTime(checkout.getCheckoutTime())
                .dueTime(checkout.getDueTime())
                .userName("%s %s".formatted(user.getFirstName(), user.getLastName()))
                .build();
    }

    /**
     * Simple method to return a list of books a user has checked out, when they got them and when they're due.
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
            return new BookDetails(book.getTitle(), checkout.getCheckoutTime(), checkout.getDueTime());
        }).toList();
        return new UserActiveCheckout("%s %s".formatted(user.getFirstName(), user.getLastName()), activeBooks);
    }

}
