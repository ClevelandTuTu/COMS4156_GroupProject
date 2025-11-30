package com.project.airhotel.room.repository;

import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Repository
public interface RoomTypeDailyPriceRepository extends JpaRepository<RoomTypeDailyPrice, Long> {
  List<RoomTypeDailyPrice> findByHotelIdAndRoomTypeIdAndStayDateBetween(
      Long hotelId,
      Long room,
      LocalDate startInclusive,
      LocalDate endInclusive);
}
