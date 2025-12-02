package com.project.airhotel.room.repository;

import com.project.airhotel.room.domain.RoomTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for RoomTypes. Provides CRUD and
 * pagination/sorting operations for room-type entities. Custom query methods
 * can be added via Spring Data derived queries or @Query.
 */
@Repository
public interface RoomTypesRepository extends JpaRepository<RoomTypes, Long> {

  /**
   * Retrieves all room types associated with a specific hotel.
   *
   * @param hotelId the unique identifier of the hotel
   * @return a list of {@link RoomTypes} belonging to the specified hotel
   */
  List<RoomTypes> findByHotelId(Long hotelId);

}
