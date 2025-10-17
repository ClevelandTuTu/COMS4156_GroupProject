package com.project.airhotel.repository;

import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface RoomsRepository extends JpaRepository<Rooms, Long> {
  @Query("SELECT r FROM Rooms r WHERE r.hotel_id = :hotelId")
  List<Rooms> findByHotelId(@Param("hotelId") Long hotelId);

  @Query("SELECT r FROM Rooms r WHERE r.hotel_id = :hotelId AND r.status = :status")
  List<Rooms> findByHotelIdAndStatus(@Param("hotelId") Long hotelId,
                                     @Param("status") RoomStatus status);

  @Query("SELECT COUNT(r) > 0 FROM Rooms r WHERE r.hotel_id = :hotelId AND r.room_number = :roomNumber")
  boolean existsByHotelIdAndRoomNumber(@Param("hotelId") Long hotelId,
                                       @Param("roomNumber") String roomNumber);
}
