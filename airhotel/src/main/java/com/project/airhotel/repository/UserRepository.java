package com.project.airhotel.repository;

import com.project.airhotel.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findTopByOrderByIdAsc();
}
