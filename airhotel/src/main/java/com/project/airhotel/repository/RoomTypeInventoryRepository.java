package com.project.airhotel.repository;

import com.project.airhotel.model.RoomTypeInventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.*;

public interface RoomTypeInventoryRepository extends JpaRepository<RoomTypeInventory, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select i from RoomTypeInventory i
      where i.hotel_id = :hotelId and i.room_type_id = :roomTypeId and i.stay_date = :stayDate
      """)
  Optional<RoomTypeInventory> findForUpdate(@Param("hotelId") Long hotelId,
                                            @Param("roomTypeId") Long roomTypeId,
                                            @Param("stayDate") LocalDate stayDate);
}
