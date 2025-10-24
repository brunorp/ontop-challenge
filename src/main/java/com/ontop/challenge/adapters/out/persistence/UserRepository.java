package com.ontop.challenge.adapters.out.persistence;

import com.ontop.challenge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity with security-focused queries
 * 
 * Security notes:
 * - All queries use parameterized statements to prevent SQL injection
 * - Spring Data JPA automatically protects against SQL injection
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username
     * Used for authentication
     * 
     * @param username the username (case-sensitive)
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if username already exists
     * Used during registration to prevent duplicates
     * 
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
}

