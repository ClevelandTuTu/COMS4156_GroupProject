package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import com.project.airhotel.room.repository.RoomTypeDailyPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ReservationPricingService {
  private final RoomTypeDailyPriceRepository roomTypeDailyPriceRepository;

  public void recalcTotalPriceOrThrow(final Reservations r) {
    final LocalDate checkIn = r.getCheckInDate();
    final LocalDate checkOut = r.getCheckOutDate();
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("Check-in and check-out dates are " +
          "required to calculate price.");
    }

    final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    if (nights <= 0) {
      throw new BadRequestException("Check out date must be later than check " +
          "in date.");
    }

    final LocalDate endInclusive = checkOut.minusDays(1);

    final List<RoomTypeDailyPrice> dailyPrices =
        roomTypeDailyPriceRepository.findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, endInclusive);

    if (dailyPrices.isEmpty()) {
      throw new BadRequestException("No daily price found for the selected " +
          "stay range.");
    }

    final Map<LocalDate, RoomTypeDailyPrice> priceByDate =
        dailyPrices.stream()
            .collect(Collectors.toMap(RoomTypeDailyPrice::getStayDate, p -> p));

    BigDecimal total = BigDecimal.ZERO;
    LocalDate d = checkIn;
    while (d.isBefore(checkOut)) {
      final RoomTypeDailyPrice p = priceByDate.get(d);
      if (p == null) {
        throw new BadRequestException("Missing daily price for date: " + d);
      }
      total = total.add(p.getPrice());
      d = d.plusDays(1);
    }

    r.setPriceTotal(total);
  }

}
