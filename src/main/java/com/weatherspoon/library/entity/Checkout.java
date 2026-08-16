package com.weatherspoon.library.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A record of one book being checked out by one user. Acts as the join between
 * {@link User} and {@link Book}, and — unlike a flat list of checked-out book IDs on
 * {@code User} — preserves history: a checkout row lives on after {@code returnTime} is
 * set, rather than being deleted, so past rentals remain queryable.
 */
@Entity
@Table(name="checkouts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checkout {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkoutId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Instant checkoutTime;

    @Column
    private Instant returnTime;

    @Column
    private Instant dueTime;

    /**
     * Starts a new checkout for the given user/book, due back in 10 days.
     */
    public Checkout(User user, Book book) {
        this.book = book;
        this.user = user;
        this.checkoutTime = Instant.now();
        this.dueTime = checkoutTime.plus(10, ChronoUnit.DAYS);
    }

    /**
     * @return true if the book has not yet been returned
     */
    public boolean isActive() {
        return returnTime == null;
    }

    /**
     * @return true if the book is still checked out and past its due date
     */
    public boolean isLate() {
        return returnTime == null && Instant.now().isAfter(dueTime);
    }

    /**
     * Equal by ID only, and only once both sides have a generated ID; two unsaved
     * instances are never equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Checkout checkout)) return false;
        return checkoutId != null && checkoutId.equals(checkout.checkoutId);
    }

    /**
     * Constant rather than ID-based, so an entity's hash code doesn't change when
     * Hibernate assigns its ID after the initial insert (which would otherwise silently
     * break it inside a {@link java.util.HashSet}/{@link java.util.HashMap}).
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
