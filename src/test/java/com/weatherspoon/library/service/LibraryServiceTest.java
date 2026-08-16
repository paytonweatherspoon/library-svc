package com.weatherspoon.library.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.weatherspoon.library.entity.TestDataFactory.createTestBook;
import static com.weatherspoon.library.entity.TestDataFactory.createTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LibraryService}. Repositories are mocked, so these exercise
 * only the service's business logic (availability checks, checkout/return bookkeeping,
 * exception mapping) independent of Spring or the database.
 */
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private CheckoutRepository checkoutRepo;

    @Mock
    private BookRepository bookRepo;

    @Mock
    private UserRepository userRepo;

    private LibraryService libraryService;

    private Book book;
    private User user;
    private User otherUser;

    private static final Long BOOK_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;


    /**
     * Creates a test book and two test users with fixed IDs, bypassing the database.
     */
    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(checkoutRepo, bookRepo, userRepo);
        book = createTestBook(BOOK_ID, "Effective Java", "Joshua Bloch", "978-0134685991");
        user = createTestUser(USER_ID, "Test", "User", "test@test.com");
        otherUser = createTestUser(OTHER_USER_ID, "New", "User", "new@test.com");
    }

    // ---------------------------------------------------------------
    // checkoutBook()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Checkout should succeed when user and book exist")
    void checkoutBookSuccess() {
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        CheckoutResponse response = libraryService.checkoutBook(request);

        assertNotNull(response);
        assertEquals("Effective Java", response.bookName());
        assertEquals("Test User", response.userName());
        assertFalse(book.getIsAvailable(), "Book should be marked unavailable after checkout");

        ArgumentCaptor<Checkout> checkoutCaptor = ArgumentCaptor.forClass(Checkout.class);
        verify(checkoutRepo, times(1)).save(checkoutCaptor.capture());
        assertEquals(book, checkoutCaptor.getValue().getBook());
        assertEquals(user, checkoutCaptor.getValue().getUser());
    }

    @Test
    @DisplayName("Checkout fails with exception when book already checked out")
    void checkoutBookFailsIfAlreadyCheckedOut() {
        book.setIsAvailable(false);
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(BookUnavailableException.class, () -> libraryService.checkoutBook(request));

        verify(checkoutRepo, never()).save(any(Checkout.class));
    }

    @Test
    @DisplayName("Checkout throws UserNotFoundException for invalid user")
    void checkoutBookThrowsUserNotFoundException() {
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> libraryService.checkoutBook(request));

        verify(checkoutRepo, never()).save(any(Checkout.class));
    }

    @Test
    @DisplayName("Checkout throws BookNotFoundException for invalid book")
    void checkoutBookThrowsBookNotFoundException() {
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> libraryService.checkoutBook(request));

        verify(userRepo, never()).findById(anyLong());
        verify(checkoutRepo, never()).save(any(Checkout.class));
    }

    // ---------------------------------------------------------------
    // returnBook()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Return fails with exception when no active checkout exists for this book")
    void returnBookFailsIfNotCurrentlyCheckedOut() {
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(checkoutRepo.getActiveCheckoutByBook(BOOK_ID)).thenReturn(Optional.empty());

        assertThrows(BookNotCheckedOutException.class, () -> libraryService.returnBook(request));

        verify(checkoutRepo, never()).save(any(Checkout.class));
    }

    @Test
    @DisplayName("Return succeeds when active checkout exists for same user")
    void returnBookSuccess() {
        book.setIsAvailable(false);
        Checkout activeCheckout = new Checkout(user, book);
        LibraryRequest request = new LibraryRequest(USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(checkoutRepo.getActiveCheckoutByBook(BOOK_ID)).thenReturn(Optional.of(activeCheckout));

        ReturnResponse response = libraryService.returnBook(request);

        assertNotNull(response);
        assertEquals("Effective Java", response.bookReturned());
        assertEquals("Test User", response.returningUser());
        assertNotNull(response.returnTime());
        assertTrue(book.getIsAvailable(), "Book should be marked available again after return");
        assertNotNull(activeCheckout.getReturnTime(), "Checkout should be stamped with a return time");

        verify(checkoutRepo, times(1)).save(activeCheckout);
    }

    @Test
    @DisplayName("Return succeeds (with a warning logged) even when the returning user differs from the original borrower")
    void returnBehaviorWhenOwnershipMismatch() {
        book.setIsAvailable(false);
        // Checkout was originally made by `user`, but `otherUser` is the one returning it.
        Checkout activeCheckout = new Checkout(user, book);
        LibraryRequest request = new LibraryRequest(OTHER_USER_ID, BOOK_ID);

        when(bookRepo.findById(BOOK_ID)).thenReturn(Optional.of(book));
        when(userRepo.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(checkoutRepo.getActiveCheckoutByBook(BOOK_ID)).thenReturn(Optional.of(activeCheckout));

        ReturnResponse response = libraryService.returnBook(request);

        assertNotNull(response);
        assertTrue(book.getIsAvailable());
        // returningUser reflects the requesting user (otherUser), not the original borrower.
        assertEquals("New User", response.returningUser());
        verify(checkoutRepo, times(1)).save(activeCheckout);
    }

    // ---------------------------------------------------------------
    // getBooksCheckedOutByUser()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Get active checkouts returns all currently checked out books for a user")
    void getBooksCheckedOutByUserSuccess() {
        Book secondBook = new Book("Clean Code", "Robert C. Martin", "978-0132350884",
                LocalDate.of(2008, 8, 1), false);

        Checkout firstCheckout = new Checkout(user, book);
        Checkout secondCheckout = new Checkout(user, secondBook);

        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(checkoutRepo.getActiveCheckoutsByUser(USER_ID))
                .thenReturn(List.of(firstCheckout, secondCheckout));

        UserActiveCheckout result = libraryService.getBooksCheckedOutByUser(USER_ID);

        assertNotNull(result);
        assertEquals("Test User", result.userName());
        assertEquals(2, result.books().size());
        assertTrue(result.books().stream().anyMatch(b -> b.title().equals("Effective Java")));
        assertTrue(result.books().stream().anyMatch(b -> b.title().equals("Clean Code")));
    }

    @Test
    @DisplayName("Get active checkouts returns an empty list when the user has none")
    void getBooksCheckedOutByUserReturnsEmptyListWhenNoneActive() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        when(checkoutRepo.getActiveCheckoutsByUser(USER_ID)).thenReturn(List.of());

        UserActiveCheckout result = libraryService.getBooksCheckedOutByUser(USER_ID);

        assertNotNull(result);
        assertTrue(result.books().isEmpty());
    }

    @Test
    @DisplayName("Get user checkouts throws UserNotFoundException if no such user")
    void getBooksCheckedOutByUserThrowsNotFoundException() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> libraryService.getBooksCheckedOutByUser(USER_ID));

        verify(checkoutRepo, never()).getActiveCheckoutsByUser(anyLong());
    }
}