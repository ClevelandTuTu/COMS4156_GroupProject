package com.project.airhotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.service.ReservationPricingService;
import com.project.airhotel.room.domain.RoomTypeDailyPrice;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypeDailyPriceRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ReservationPricingService.
 */
@ExtendWith(MockitoExtension.class)
class ReservationPricingServiceTest {

  @Mock
  private RoomTypeDailyPriceRepository roomTypeDailyPriceRepository;

  @Mock
  private RoomTypesRepository roomTypesRepository;

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

  private RoomTypes roomTypeWithBaseRate(final String baseRate) {
    final RoomTypes rt = new RoomTypes();
    rt.setBaseRate(new BigDecimal(baseRate));
    return rt;
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → sums all nightly prices for stay range")
  void recalcTotalPrice_happyPath() {
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 9);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    when(roomTypesRepository.findById(roomTypeId))
        .thenReturn(Optional.of(roomTypeWithBaseRate("9999.99")));

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

    pricingService.recalcTotalPriceOrThrow(r);

    assertNotNull(r.getPriceTotal());
    assertEquals(new BigDecimal("450.00"), r.getPriceTotal());

    verify(roomTypesRepository).findById(roomTypeId);
    verify(roomTypeDailyPriceRepository)
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1));
    verify(roomTypeDailyPriceRepository, never()).saveAll(anyList());
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

    verifyNoInteractions(roomTypesRepository, roomTypeDailyPriceRepository);
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → checkOut not after checkIn throws")
  void recalcTotalPrice_invalidOrder_throw() {
    final Reservations rEqual = baseReservationWithDates(
        LocalDate.of(2026, 1, 6),
        LocalDate.of(2026, 1, 6));

    final Reservations rBefore = baseReservationWithDates(
        LocalDate.of(2026, 1, 7),
        LocalDate.of(2026, 1, 6));

    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(rEqual));
    assertThrows(BadRequestException.class,
        () -> pricingService.recalcTotalPriceOrThrow(rBefore));

    verifyNoInteractions(roomTypesRepository, roomTypeDailyPriceRepository);
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → no daily prices → use baseRate for all nights")
  void recalcTotalPrice_noDailyPrices_useBaseRate() {
    // Given stay [2026-01-06, 2026-01-08) → dates: 6,7 → 2 nights
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 8);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);
    final Long roomTypeId = r.getRoomTypeId();

    when(roomTypesRepository.findById(roomTypeId))
        .thenReturn(Optional.of(roomTypeWithBaseRate("100.00")));

    when(roomTypeDailyPriceRepository
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            r.getHotelId(), r.getRoomTypeId(), checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of());

    pricingService.recalcTotalPriceOrThrow(r);

    assertNotNull(r.getPriceTotal());
    assertEquals(new BigDecimal("200.00"), r.getPriceTotal());

    verify(roomTypesRepository).findById(roomTypeId);
    verify(roomTypeDailyPriceRepository)
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            r.getHotelId(), r.getRoomTypeId(), checkIn, checkOut.minusDays(1));
    verify(roomTypeDailyPriceRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("recalcTotalPriceOrThrow → missing some nightly prices → fallback to baseRate")
  void recalcTotalPrice_missingDate_fallbackToBaseRate() {
    final LocalDate checkIn = LocalDate.of(2026, 1, 6);
    final LocalDate checkOut = LocalDate.of(2026, 1, 9);
    final Reservations r = baseReservationWithDates(checkIn, checkOut);
    final Long hotelId = r.getHotelId();
    final Long roomTypeId = r.getRoomTypeId();

    when(roomTypesRepository.findById(roomTypeId))
        .thenReturn(Optional.of(roomTypeWithBaseRate("150.00")));

    final RoomTypeDailyPrice p1 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 6), "100.00");
    final RoomTypeDailyPrice p3 = dailyPrice(hotelId, roomTypeId,
        LocalDate.of(2026, 1, 8), "200.00");

    when(roomTypeDailyPriceRepository
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of(p1, p3));

    // When
    pricingService.recalcTotalPriceOrThrow(r);

    assertNotNull(r.getPriceTotal());
    assertEquals(new BigDecimal("450.00"), r.getPriceTotal());

    verify(roomTypesRepository).findById(roomTypeId);
    verify(roomTypeDailyPriceRepository)
        .findByHotelIdAndRoomTypeIdAndStayDateBetween(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1));
    verify(roomTypeDailyPriceRepository).saveAll(anyList());
  }
}
