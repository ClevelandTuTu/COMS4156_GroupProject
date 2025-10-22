package com.project.airhotel.repository;

import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Rooms. Provides CRUD operations plus custom
 * queries scoped by hotel, status filtering, and room-number existence checks
 * within a hotel.
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Repository
public interface RoomsRepository extends JpaRepository<Rooms, Long> {

  /**
   * Returns all rooms that belong to the specified hotel.
   *
   * @param hotelId hotel identifier
   * @return list of rooms under the hotel
   */
  @Query("SELECT r FROM Rooms r WHERE r.hotel_id = :hotelId")
  List<Rooms> findByHotelId(@Param("hotelId") Long hotelId);

  /**
   * Returns rooms for a hotel filtered by status.
   *
   * @param hotelId hotel identifier
   * @param status  desired room status
   * @return list of rooms matching the hotel and status
   */
  @Query("SELECT r FROM Rooms r WHERE r.hotel_id = :hotelId AND r.status = "
      + ":status")
  List<Rooms> findByHotelIdAndStatus(@Param("hotelId") Long hotelId,
                                     @Param("status") RoomStatus status);

  /**
   * Checks whether a room number already exists within the given hotel. Useful
   * for enforcing per-hotel room-number uniqueness.
   *
   * @param hotelId    hotel identifier
   * @param roomNumber room number to test
   * @return true if a room with the same number exists under the hotel, false
   * otherwise
   */
  @Query("SELECT COUNT(r) > 0 FROM Rooms r WHERE r.hotel_id = :hotelId AND r"
      + ".room_number = :roomNumber")
  boolean existsByHotelIdAndRoomNumber(@Param("hotelId") Long hotelId,
                                       @Param("roomNumber") String roomNumber);
}
