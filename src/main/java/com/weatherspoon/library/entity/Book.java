package com.weatherspoon.library.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A book in the library's catalog. {@code isAvailable} reflects whether the book is
 * currently checked out; {@code version} backs optimistic locking so two concurrent
 * checkouts of the same book can't both succeed.
 */
@Entity
@Table(name="books")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column
    private String isbn;

    @Column
    private LocalDate publishedDate;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    /**
     * Creates a new, available book.
     */
    public Book(String title, String author, String isbn, LocalDate publishedDate) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedDate = publishedDate;
    }

    /**
     * Creates a new book with an explicit initial availability.
     */
    public Book(String title, String author, String isbn, LocalDate publishedDate, Boolean isAvailable) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedDate = publishedDate;
        this.isAvailable = isAvailable;
    }

    /**
     * Package-private constructor for tests that need to fix the generated ID without
     * round-tripping through the database.
     */
    Book(Long bookId, String title, String author, String isbn, LocalDate publishedDate) {
        this(title, author, isbn, publishedDate);
        this.bookId = bookId;
    }

    /**
     * Equal by ID only, and only once both sides have a generated ID; two unsaved
     * instances are never equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book book)) return false;
        return bookId != null && bookId.equals(book.bookId);
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
