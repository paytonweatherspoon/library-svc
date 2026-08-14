package com.weatherspoon.library.repository;

import com.weatherspoon.library.entity.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CRUD access to {@link Checkout}, plus lookups for the currently-active (not yet
 * returned) checkouts, which drive the checkout/return/list-active-books flows.
 */
@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Long> {

    /**
     * @return all books the given user currently has checked out (not yet returned)
     */
    @Query("SELECT c FROM Checkout c WHERE c.user.userId = :userId AND c.returnTime IS NULL")
    List<Checkout> getActiveCheckoutsByUser(Long userId);

    /**
     * @return the given book's active checkout, if it's currently checked out to anyone
     */
    @Query("SELECT c FROM Checkout c WHERE c.book.bookId = :bookId AND c.returnTime IS NULL")
    Optional<Checkout> getActiveCheckoutByBook(Long bookId);
}
