package com.weatherspoon.library.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A library patron who can check out and return books.
 */
@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Creates a new user.
     */
    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Package-private constructor for tests that need to fix the generated ID without
     * round-tripping through the database.
     */
    User(Long userId, String firstName, String lastName, String email) {
        this(firstName, lastName, email);
        this.userId = userId;
    }

    /**
     * Equal by ID only, and only once both sides have a generated ID; two unsaved
     * instances are never equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return userId != null && userId.equals(user.userId);
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
