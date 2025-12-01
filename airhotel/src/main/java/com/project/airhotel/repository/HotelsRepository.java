package com.project.airhotel.repository;

import com.project.airhotel.model.Hotels;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * Fuzzy search by city name prefix (case-insensitive).
   * Example: keyword = "new" matches "New York", "New Haven", "New Orleans".
   *
   * We normalize by trimming and lowering case to avoid issues with
   * leading/trailing spaces or case differences.
   *
   * NOTE: we use prefix match: city LIKE :keyword% (not %keyword%),
   *       to avoid weird matches like "ReNew Hotel".
   *
   * @param keyword partial city prefix, e.g. "new"
   * @return list of hotels whose city starts with the keyword
   */
  @Query("""
      select h
        from Hotels h
       where lower(trim(h.city)) like lower(concat(:keyword, '%'))
      """)
  List<Hotels> searchCityByPrefix(@Param("keyword") String keyword);
}
