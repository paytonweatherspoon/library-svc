package com.weatherspoon.library.repository;

import com.weatherspoon.library.entity.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD access to {@link Checkout}, plus a lookup for the currently-active (not yet
 * returned) checkouts for a user, which drives the list-active-checkouts flow. Returning
 * a book is looked up directly by checkout ID via the inherited {@code findById}.
 */
@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Long> {

    /**
     * @return all books the given user currently has checked out (not yet returned)
     */
    @Query("SELECT c FROM Checkout c WHERE c.user.userId = :userId AND c.returnTime IS NULL")
    List<Checkout> getActiveCheckoutsByUser(Long userId);
}
