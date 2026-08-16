package com.weatherspoon.library.controller;

import com.weatherspoon.library.dto.BookDetails;
import com.weatherspoon.library.dto.CheckoutDetails;
import com.weatherspoon.library.dto.request.LibraryRequest;
import com.weatherspoon.library.dto.request.ReturnCheckoutRequest;
import com.weatherspoon.library.dto.response.CheckoutResponse;
import com.weatherspoon.library.dto.response.ReturnResponse;
import com.weatherspoon.library.dto.response.UserActiveCheckout;
import com.weatherspoon.library.exception.BookNotFoundException;
import com.weatherspoon.library.exception.BookUnavailableException;
import com.weatherspoon.library.exception.CheckoutAlreadyReturnedException;
import com.weatherspoon.library.exception.CheckoutNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link LibraryController}. LibraryService is mocked so these
 * exercise only routing, request validation, JSON (de)serialization, and the
 * exception -> HTTP status mapping in GlobalExceptionHandler.
 */
@WebMvcTest(LibraryController.class)
class LibraryControllerTest {

    private static final String CHECKOUTS_URL = "/api/v1/checkouts";

    private static final Long USER_ID = 1L;
    private static final Long BOOK_ID = 1L;
    private static final Long CHECKOUT_ID = 1L;

    private static String returnUrl(Long checkoutId) {
        return CHECKOUTS_URL + "/" + checkoutId + "/return";
    }

    private static String userCheckoutsUrl(Long userId) {
        return "/api/v1/users/" + userId + "/checkouts";
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // POST /api/v1/checkouts
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /checkouts returns 201 with a Location header and the checkout details when the book is available")
    void checkoutBook_returnsCreated_whenValid() throws Exception {
        Instant checkoutTime = Instant.parse("2026-08-14T10:00:00Z");
        Instant dueTime = checkoutTime.plusSeconds(864000);
        CheckoutResponse response = CheckoutResponse.builder()
                .checkoutId(CHECKOUT_ID)
                .userName("Test User")
                .bookName("Effective Java")
                .checkoutTime(checkoutTime)
                .dueTime(dueTime)
                .build();
        when(libraryService.checkoutBook(any(LibraryRequest.class))).thenReturn(response);

        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", CHECKOUTS_URL + "/" + CHECKOUT_ID))
                .andExpect(jsonPath("$.checkoutId").value(CHECKOUT_ID))
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.bookName").value("Effective Java"))
                .andExpect(jsonPath("$.checkoutTime").exists())
                .andExpect(jsonPath("$.dueTime").exists());
    }

    @Test
    @DisplayName("POST /checkouts returns 404 when the book does not exist")
    void checkoutBook_returnsNotFound_whenBookNotFound() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new BookNotFoundException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book %d does not exist".formatted(BOOK_ID)));
    }

    @Test
    @DisplayName("POST /checkouts returns 404 when the user does not exist")
    void checkoutBook_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /checkouts returns 409 when the book is already checked out")
    void checkoutBook_returnsConflict_whenBookUnavailable() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new BookUnavailableException(BOOK_ID, Instant.now()));

        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /checkouts returns 409 when a concurrent request already modified the book's version")
    void checkoutBook_returnsConflict_whenOptimisticLockFailure() throws Exception {
        when(libraryService.checkoutBook(any(LibraryRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("books", BOOK_ID));

        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, BOOK_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The book was modified by another request. Please retry."));
    }

    @Test
    @DisplayName("POST /checkouts returns 422 and never calls the service when userId is missing")
    void checkoutBook_returnsUnprocessableEntity_whenUserIdNull() throws Exception {
        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(null, BOOK_ID))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).checkoutBook(any(LibraryRequest.class));
    }

    @Test
    @DisplayName("POST /checkouts returns 422 and never calls the service when bookId is not positive")
    void checkoutBook_returnsUnprocessableEntity_whenBookIdNotPositive() throws Exception {
        mockMvc.perform(post(CHECKOUTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LibraryRequest(USER_ID, -1L))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).checkoutBook(any(LibraryRequest.class));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/checkouts/{checkoutId}/return
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /checkouts/{id}/return returns 200 with the return details when the checkout is active")
    void returnBook_returnsOk_whenValid() throws Exception {
        ReturnResponse response = ReturnResponse.builder()
                .checkoutId(CHECKOUT_ID)
                .returningUser("Test User")
                .bookReturned("Effective Java")
                .returnTime(Instant.parse("2026-08-14T10:00:00Z").toString())
                .build();
        when(libraryService.returnBook(eq(CHECKOUT_ID), any(ReturnCheckoutRequest.class))).thenReturn(response);

        mockMvc.perform(post(returnUrl(CHECKOUT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnCheckoutRequest(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutId").value(CHECKOUT_ID))
                .andExpect(jsonPath("$.returningUser").value("Test User"))
                .andExpect(jsonPath("$.bookReturned").value("Effective Java"));
    }

    @Test
    @DisplayName("POST /checkouts/{id}/return returns 404 when the checkout does not exist")
    void returnBook_returnsNotFound_whenCheckoutNotFound() throws Exception {
        when(libraryService.returnBook(eq(CHECKOUT_ID), any(ReturnCheckoutRequest.class)))
                .thenThrow(new CheckoutNotFoundException(CHECKOUT_ID, Instant.now()));

        mockMvc.perform(post(returnUrl(CHECKOUT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnCheckoutRequest(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /checkouts/{id}/return returns 409 when the checkout is already closed")
    void returnBook_returnsConflict_whenAlreadyReturned() throws Exception {
        when(libraryService.returnBook(eq(CHECKOUT_ID), any(ReturnCheckoutRequest.class)))
                .thenThrow(new CheckoutAlreadyReturnedException(CHECKOUT_ID, Instant.now()));

        mockMvc.perform(post(returnUrl(CHECKOUT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnCheckoutRequest(USER_ID))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /checkouts/{id}/return returns 404 when the returning user does not exist")
    void returnBook_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.returnBook(eq(CHECKOUT_ID), any(ReturnCheckoutRequest.class)))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(post(returnUrl(CHECKOUT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnCheckoutRequest(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /checkouts/{id}/return returns 422 and never calls the service for an invalid body")
    void returnBook_returnsUnprocessableEntity_whenInvalidBody() throws Exception {
        mockMvc.perform(post(returnUrl(CHECKOUT_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReturnCheckoutRequest(null))))
                .andExpect(status().isUnprocessableContent());

        verify(libraryService, never()).returnBook(anyLong(), any(ReturnCheckoutRequest.class));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/checkouts/{checkoutId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /checkouts/{id} returns 200 with the checkout's full details")
    void getCheckout_returnsOk_whenFound() throws Exception {
        CheckoutDetails details = CheckoutDetails.builder()
                .checkoutId(CHECKOUT_ID)
                .userName("Test User")
                .bookName("Effective Java")
                .checkoutTime(Instant.parse("2026-08-14T10:00:00Z"))
                .dueTime(Instant.parse("2026-08-24T10:00:00Z"))
                .returnTime(null)
                .build();
        when(libraryService.getCheckout(CHECKOUT_ID)).thenReturn(details);

        mockMvc.perform(get(CHECKOUTS_URL + "/" + CHECKOUT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutId").value(CHECKOUT_ID))
                .andExpect(jsonPath("$.bookName").value("Effective Java"))
                .andExpect(jsonPath("$.returnTime").doesNotExist());
    }

    @Test
    @DisplayName("GET /checkouts/{id} returns 404 when the checkout does not exist")
    void getCheckout_returnsNotFound_whenMissing() throws Exception {
        when(libraryService.getCheckout(CHECKOUT_ID))
                .thenThrow(new CheckoutNotFoundException(CHECKOUT_ID, Instant.now()));

        mockMvc.perform(get(CHECKOUTS_URL + "/" + CHECKOUT_ID))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // GET /api/v1/users/{userId}/checkouts
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /users/{id}/checkouts returns 200 with the user's active books, including each checkoutId")
    void getUserActiveCheckouts_returnsOk_whenValid() throws Exception {
        UserActiveCheckout response = new UserActiveCheckout("Test User",
                List.of(new BookDetails(CHECKOUT_ID, "Effective Java", Instant.now(), Instant.now().plusSeconds(864000))));
        when(libraryService.getBooksCheckedOutByUser(USER_ID)).thenReturn(response);

        mockMvc.perform(get(userCheckoutsUrl(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.books[0].checkoutId").value(CHECKOUT_ID))
                .andExpect(jsonPath("$.books[0].title").value("Effective Java"));
    }

    @Test
    @DisplayName("GET /users/{id}/checkouts returns 404 when the user does not exist")
    void getUserActiveCheckouts_returnsNotFound_whenUserNotFound() throws Exception {
        when(libraryService.getBooksCheckedOutByUser(USER_ID))
                .thenThrow(new UserNotFoundException(USER_ID, Instant.now()));

        mockMvc.perform(get(userCheckoutsUrl(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/{id}/checkouts returns 400 when userId is not a number")
    void getUserActiveCheckouts_returnsBadRequest_whenUserIdNotNumeric() throws Exception {
        mockMvc.perform(get(userCheckoutsUrl("not-a-number")))
                .andExpect(status().isBadRequest());

        verify(libraryService, never()).getBooksCheckedOutByUser(anyLong());
    }

    private static String userCheckoutsUrl(String rawUserId) {
        return "/api/v1/users/" + rawUserId + "/checkouts";
    }
}
