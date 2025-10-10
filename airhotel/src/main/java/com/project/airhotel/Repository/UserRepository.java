package com.project.airhotel.Repository;

import com.project.airhotel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findTopByOrderByIdAsc();
}
