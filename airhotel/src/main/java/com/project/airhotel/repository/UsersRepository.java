package com.project.airhotel.repository;

import com.project.airhotel.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Users. Provides CRUD operations and a finder
 * by email.
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {

  /**
   * Finds a user by exact email match. The Users table is expected to enforce a
   * unique constraint on the email column.
   *
   * @param email the email address to look up; must not be null or blank
   * @return an Optional containing the matching user if present; otherwise
   * empty
   */
  Optional<Users> findByEmail(String email);

}
