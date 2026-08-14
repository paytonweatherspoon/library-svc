package com.weatherspoon.library.repository;

import com.weatherspoon.library.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * CRUD access to {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
