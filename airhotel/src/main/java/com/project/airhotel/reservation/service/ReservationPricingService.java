package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypeDailyPriceRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Prices are computed by aggregating day-level room-type prices, falling back
 * to the room type base rate when no explicit daily price exists.
 */
@Service
@RequiredArgsConstructor
public class ReservationPricingService {

  private final RoomTypeDailyPriceRepository roomTypeDailyPriceRepository;
  private final RoomTypesRepository roomTypesRepository;

  /**
   * Recalculate the total price of a reservation, or throw if the inputs are invalid.
   * The method checks stay dates, retrieves the room type and its base rate,
   * loads or creates day-level prices for each night, and finally updates the
   * reservation's {@code priceTotal}.
   *
   * @param r reservation whose price should be recalculated
   * @throws BadRequestException if dates are invalid or pricing configuration is missing
   */
  public void recalcTotalPriceOrThrow(final Reservations r) {
    final LocalDate checkIn = r.getCheckInDate();
    final LocalDate checkOut = r.getCheckOutDate();
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("Check-in and check-out dates are "
          + "required to calculate price.");
    }

    final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    if (nights <= 0) {
      throw new BadRequestException("Check out date must be later than check "
          + "in date.");
    }

    final RoomTypes roomType = roomTypesRepository.findById(roomTypeId)
        .orElseThrow(() -> new BadRequestException(
            "Room type not found: " + roomTypeId
        ));

    final BigDecimal defaultDailyPrice = roomType.getBaseRate();
    if (defaultDailyPrice == null) {
      throw new BadRequestException("Base rate is not configured for room type "
          + roomTypeId);
    }

    final LocalDate endInclusive = checkOut.minusDays(1);

    final List<RoomTypeDailyPrice> dailyPrices =
        roomTypeDailyPriceRepository.findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, endInclusive);

    final Map<LocalDate, RoomTypeDailyPrice> priceByDate =
        dailyPrices.stream()
            .collect(Collectors.toMap(RoomTypeDailyPrice::getStayDate, p -> p));

    BigDecimal total = BigDecimal.ZERO;
    final List<RoomTypeDailyPrice> toSave = new ArrayList<>();

    LocalDate d = checkIn;
    while (d.isBefore(checkOut)) {
      RoomTypeDailyPrice p = priceByDate.get(d);
      if (p == null) {
        p = RoomTypeDailyPrice.builder()
            .hotelId(hotelId)
            .roomTypeId(roomTypeId)
            .stayDate(d)
            .price(defaultDailyPrice)
            .computedFrom("{\"source\":\"BASE_RATE\"}")
            .build();

        toSave.add(p);
        priceByDate.put(d, p);
      }

      total = total.add(p.getPrice());
      d = d.plusDays(1);
    }

    if (!toSave.isEmpty()) {
      roomTypeDailyPriceRepository.saveAll(toSave);
    }

    r.setPriceTotal(total);
  }

}
