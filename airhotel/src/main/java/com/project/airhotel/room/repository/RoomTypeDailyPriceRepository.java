package com.project.airhotel.room.repository;

import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for room type daily price.
 */
@Repository
public interface RoomTypeDailyPriceRepository extends JpaRepository<RoomTypeDailyPrice, Long> {
  List<RoomTypeDailyPrice> findByHotelIdAndRoomTypeIdAndStayDateBetween(
      Long hotelId,
      Long room,
      LocalDate startInclusive,
      LocalDate endInclusive);
}
