package com.bookmyshow.user.repository;

import com.bookmyshow.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given phone number.
     */
    boolean existsByPhone(String phone);
}
