package com.project.airhotel.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.service.ReservationPricingService;
import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import com.project.airhotel.room.repository.RoomTypeDailyPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationPricingService.
 */
@ExtendWith(MockitoExtension.class)
class ReservationPricingServiceTest {

  @Mock
  private RoomTypeDailyPriceRepository roomTypeDailyPriceRepository;

  @InjectMocks
  private ReservationPricingService pricingService;

  private Reservations baseReservationWithDates(final LocalDate checkIn,
                                                final LocalDate checkOut) {
    final Reservations r = new Reservations();
    r.setId(1L);
    r.setHotelId(2L);
    r.setRoomTypeId(3L);
    r.setCheckInDate(checkIn);
    r.setCheckOutDate(checkOut);
    return r;
  }

  private RoomTypeDailyPrice dailyPrice(final Long hotelId,
                                        final Long roomTypeId,
                                        final LocalDate stayDate,
                                        final String price) {
    final RoomTypeDailyPrice p = new RoomTypeDailyPrice();
    p.setHotelId(hotelId);
    p.setRoomTypeId(roomTypeId);
    p.setStayDate(stayDate);
    p.setPrice(new BigDecimal(price));
    return p;
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → sums all nightly prices for stay range")
  void recalcTotalPrice_happyPath() {
    // Given a reservation for [2026-01-06, 2026-01-09) → nights: 6,7,8
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 9);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    // And three daily prices: 100.00, 150.00, 200.00
    final RoomTypeDailyPrice p1 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 6), "100.00");
    final RoomTypeDailyPrice p2 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 7), "150.00");
    final RoomTypeDailyPrice p3 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 8), "200.00");

    when(roomTypeDailyPriceRepository
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of(p1, p2, p3));

    // When
    pricingService.recalcTotalPriceOrThrow(r);

    // Then priceTotal should be 100 + 150 + 200 = 450.00
    assertNotNull(r.getPriceTotal());
    assertEquals(new BigDecimal("450.00"), r.getPriceTotal());
    verify(roomTypeDailyPriceRepository)
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1));
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → null dates throw BadRequestException")
  void recalcTotalPrice_nullDates_throw() {
    final Reservations r1 = baseReservationWithDates(null,
        LocalDate.of(2026, 1, 7));
    final Reservations r2 = baseReservationWithDates(
        LocalDate.of(2026, 1, 6), null);

    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(r1));
    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(r2));

    // Repository must never be called when dates are invalid
    verifyNoInteractions(roomTypeDailyPriceRepository);
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → checkOut not after checkIn throws")
  void recalcTotalPrice_invalidOrder_throw() {
    // checkIn == checkOut → nights = 0
    final Reservations rEqual = baseReservationWithDates(
        LocalDate.of(2026, 1, 6),
        LocalDate.of(2026, 1, 6));

    // checkOut before checkIn → nights < 0
    final Reservations rBefore = baseReservationWithDates(
        LocalDate.of(2026, 1, 7),
        LocalDate.of(2026, 1, 6));

    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(rEqual));
    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(rBefore));

    // Repository must never be called when date range is invalid
    verifyNoInteractions(roomTypeDailyPriceRepository);
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → empty daily price result throws")
  void recalcTotalPrice_noDailyPrices_throw() {
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 8);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);

    when(roomTypeDailyPriceRepository
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            r.getHotelId(), r.getRoomTypeId(), checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of());

    final BadRequestException ex = assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(r));
    assertTrue(ex.getMessage().contains("No daily price"));

    assertNull(r.getPriceTotal());
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → missing one of the nightly prices throws")
  void recalcTotalPrice_missingDate_throw() {
    // Given stay [2026-01-06, 2026-01-09) → dates: 6,7,8
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 9);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    // Repository returns only prices for 6th and 8th, missing 7th
    final RoomTypeDailyPrice p1 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 6), "100.00");
    final RoomTypeDailyPrice p3 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 8), "200.00");

    when(roomTypeDailyPriceRepository
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of(p1, p3));

    final BadRequestException ex = assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(r));

    // The message should mention the missing date
    assertTrue(ex.getMessage().contains("Missing daily price for date"));

    // Reservation priceTotal should not be changed
    assertNull(r.getPriceTotal());
  }
}
