package com.weatherspoon.library.controller;

import com.weatherspoon.library.dto.BookDetails;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.exception.BookNotCheckedOutException;
import com.weatherspoon.library.exception.BookNotFoundException;
import com.weatherspoon.library.exception.BookUnavailableException;
import com.weatherspoon.library.exception.UserNotFoundException;
import com.weatherspoon.library.service.LibraryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link LibraryController}. LibraryService is mocked so these
 * exercise only routing, request validation, JSON (de)serialization, and the
 * exception -> HTTP status mapping in GlobalExceptionHandler.
 */
@WebMvcTest(LibraryController.class)
class LibraryControllerTest {

    private static final String CHECKOUT_URL = "/api/v1/library/checkout";
    private static final String RETURN_URL = "/api/v1/library/return";
    private static final String ACTIVE_CHECKOUTS_URL = "/api/v1/library/userActiveCheckouts";

    private static final Long USER_ID = 1L;
    private static final Long BOOK_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // POST /checkout
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /checkout returns 201 with the checkout details when the book is available")
    void checkoutBook_returnsCreated_whenValid() throws Exception {
        Instant checkoutTime = Instant.parse("2026-08-14T10:00:00Z");
        Instant dueTime = checkoutTime.plusSeconds(864000);
        CheckoutResponse response = CheckoutResponse.builder()
                .userName("Test User")
                .bookName("Effective Java")
                .checkoutTime(checkoutTime)
                .dueTime(dueTime)
                .build();
        when(libraryService.checkoutBook(any(LibraryRequest.class))).thenReturn(response);

        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.bookName").value("Effective Java"))
                .andExpect(jsonPath("$.checkoutTime").exists())
                .andExpect(jsonPath("$.dueTime").exists());
    }

    @Test
    @DisplayName("POST /checkout returns 404 when the book does not exist")
    void checkoutBook_returnsNotFound_whenBookNotFound() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new BookNotFoundException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book %d does not exist".formatted(BOOK_ID)));
    }

    @Test
    @DisplayName("POST /checkout returns 404 when the user does not exist")
    void checkoutBook_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /checkout returns 409 when the book is already checked out")
    void checkoutBook_returnsConflict_whenBookUnavailable() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new BookUnavailableException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /checkout returns 409 when a concurrent request already modified the book's version")
    void checkoutBook_returnsConflict_whenOptimisticLockFailure() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("books", BOOK_ID));

        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The book was modified by another request. Please retry."));
    }

    @Test
    @DisplayName("POST /checkout returns 422 and never calls the service when userId is missing")
    void checkoutBook_returnsUnprocessableEntity_whenUserIdNull() throws Exception {
        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(null, BOOK_ID))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).checkoutBook(any(LibraryRequest.class));
    }

    @Test
    @DisplayName("POST /checkout returns 422 and never calls the service when bookId is not positive")
    void checkoutBook_returnsUnprocessableEntity_whenBookIdNotPositive() throws Exception {
        mockMvc.perform(post(CHECKOUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, -1L))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).checkoutBook(any(LibraryRequest.class));
    }

    // ---------------------------------------------------------------
    // POST /return
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /return returns 200 with the return details when the checkout is active")
    void returnBook_returnsOk_whenValid() throws Exception {
        ReturnResponse response = ReturnResponse.builder()
                .returningUser("Test User")
                .bookReturned("Effective Java")
                .returnTime(Instant.parse("2026-08-14T10:00:00Z").toString())
                .build();
        when(libraryService.returnBook(any(LibraryRequest.class))).thenReturn(response);

        mockMvc.perform(post(RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returningUser").value("Test User"))
                .andExpect(jsonPath("$.bookReturned").value("Effective Java"));
    }

    @Test
    @DisplayName("POST /return returns 400 when the book has no active checkout")
    void returnBook_returnsBadRequest_whenNotCheckedOut() throws Exception {
        when(libraryService.returnBook(any(LibraryRequest.class)))
                .thenThrow(new BookNotCheckedOutException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /return returns 404 when the book does not exist")
    void returnBook_returnsNotFound_whenBookNotFound() throws Exception {
        when(libraryService.returnBook(any(LibraryRequest.class)))
                .thenThrow(new BookNotFoundException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /return returns 404 when the user does not exist")
    void returnBook_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.returnBook(any(LibraryRequest.class)))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(post(RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /return returns 422 and never calls the service for an invalid body")
    void returnBook_returnsUnprocessableEntity_whenInvalidBody() throws Exception {
        mockMvc.perform(post(RETURN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, null))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).returnBook(any(LibraryRequest.class));
    }

    // ---------------------------------------------------------------
    // GET /userActiveCheckouts
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /userActiveCheckouts returns 200 with the user's active books")
    void getUserActiveCheckouts_returnsOk_whenValid() throws Exception {
        UserActiveCheckout response = new UserActiveCheckout("Test User",
                List.of(new BookDetails("Effective Java", Instant.now(), Instant.now().plusSeconds(864000))));
        when(libraryService.getBooksCheckedOutByUser(USER_ID)).thenReturn(response);

        mockMvc.perform(get(ACTIVE_CHECKOUTS_URL).param("userId", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.books[0].title").value("Effective Java"));
    }

    @Test
    @DisplayName("GET /userActiveCheckouts returns 404 when the user does not exist")
    void getUserActiveCheckouts_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.getBooksCheckedOutByUser(USER_ID))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(get(ACTIVE_CHECKOUTS_URL).param("userId", USER_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /userActiveCheckouts returns 400 when userId is missing")
    void getUserActiveCheckouts_returnsBadRequest_whenUserIdMissing() throws Exception {
        mockMvc.perform(get(ACTIVE_CHECKOUTS_URL))
                .andExpect(status().isBadRequest());

        verify(libraryService, never()).getBooksCheckedOutByUser(anyLong());
    }

    @Test
    @DisplayName("GET /userActiveCheckouts returns 400 when userId is not a number")
    void getUserActiveCheckouts_returnsBadRequest_whenUserIdNotNumeric() throws Exception {
        mockMvc.perform(get(ACTIVE_CHECKOUTS_URL).param("userId", "not-a-number"))
                .andExpect(status().isBadRequest());

        verify(libraryService, never()).getBooksCheckedOutByUser(anyLong());
    }
}