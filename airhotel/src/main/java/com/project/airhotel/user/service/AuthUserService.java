package com.project.airhotel.user.service;

import com.project.airhotel.user.domain.Users;
import com.project.airhotel.user.repository.UsersRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for resolving an internal Users record from an
 * authenticated principal and creating one if necessary. The lookup is
 * performed by email, which is expected to be unique. Typical usage is in the
 * OAuth2 login flow to ensure the application has a stable user id.
 */
@Service
public class AuthUserService {
  /** Repository for user data. */
  private final UsersRepository usersRepository;

  /**
   * Constructs the service with a UsersRepository dependency.
   *
   * @param usersRepo repository used to query and persist Users entities
   */
  public AuthUserService(final UsersRepository usersRepo) {
    this.usersRepository = usersRepo;
  }

  /**
   * Finds a user by the given email. If no user exists, a new record is created
   * using the provided email and name. If a user exists and the provided name
   * is non-null and different from the stored one, the name is updated.
   * <p>
   * Transactional semantics: the read-modify-write cycle executes within a
   * single transaction. It is expected that the underlying Users table enforces
   * a unique constraint on email.
   * <p>
   * Preconditions:
   * - email must be non-null and non-blank
   * <p>
   * Side effects:
   * - May insert a new Users row if email does not exist
   * - May update the name field of an existing user if a different non-null
   * name is provided
   *
   * @param email the unique email used to identify the user; must not be null
   *              or blank
   * @param name  an optional display name; if non-null and different from the
   *              stored value, it will be saved
   * @return the id of the resolved or newly created Users entity
   * @throws IllegalStateException if email is null or blank
   */
  @Transactional
  public Long findOrCreateByEmail(final String email,
                                  @Nullable final String name) {
    if (email == null || email.isBlank()) {
      throw new IllegalStateException("OAuth2 principal has no email.");
    }
    final Users user = usersRepository.findByEmail(email)
        .map(u -> {
          if (name != null && !name.equals(u.getName())) {
            u.setName(name);
          }
          return u;
        })
        .orElseGet(() -> {
          final Users u = new Users();
          u.setEmail(email);
          u.setName(name);
          return usersRepository.save(u);
        });
    return user.getId();
  }
}
