package com.weatherspoon.library.repository;

import com.weatherspoon.library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CRUD access to {@link Book}. All queries beyond basic CRUD are covered by Spring
 * Data JPA's generated implementation, so no custom methods are needed here.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
