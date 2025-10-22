package com.project.airhotel.repository;

import com.project.airhotel.model.RoomTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for RoomTypes. Provides CRUD and
 * pagination/sorting operations for room-type entities. Custom query methods
 * can be added via Spring Data derived queries or @Query.
 */
@Repository
public interface RoomTypesRepository extends JpaRepository<RoomTypes, Long> {
}
