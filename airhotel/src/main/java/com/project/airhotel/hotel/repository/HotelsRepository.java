package com.project.airhotel.hotel.repository;

import com.project.airhotel.hotel.domain.Hotels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Hotels. Provides CRUD and pagination/sorting
 * operations for Hotels entities. The entity identifier type is Long.
 * <p>
 * Additional query methods can be added using Spring Data derived queries or
 * Query annotations when needed.
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Repository
public interface HotelsRepository extends JpaRepository<Hotels, Long> {

  /**
   * Checks whether a hotel exists by its unique identifier.
   *
   * @param id the hotel ID to check
   * @return true if a hotel with the given ID exists, false otherwise
   */
  boolean existsById(Long id);
}
