package com.project.airhotel.service.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.service.ReservationNightsService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReservationNightsService. Each test explicitly states which method & branch is
 * being tested.
 */
class ReservationNightsServiceTest {

  private final ReservationNightsService service = new ReservationNightsService();

  // -------------------------- recalcNightsOrThrow --------------------------

  @Test
  @DisplayName("recalcNightsOrThrow → branch: null dates (check-in null)")
  void recalcNights_nullCheckIn_throws() {
    final Reservations r = new Reservations();
    final LocalDate out = LocalDate.now().plusDays(1);

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, null, out)
    );
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: null dates (check-out null)")
  void recalcNights_nullCheckOut_throws() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.now();

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, null)
    );
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: invalid range (check-out equals check-in)")
  void recalcNights_equalDates_throws() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 1, 10);

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, in)
    );
    assertTrue(ex.getMessage().contains("later than"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: invalid range (check-out before check-in)")
  void recalcNights_outBeforeIn_throws() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 1, 10);
    final LocalDate out = LocalDate.of(2025, 1, 9);

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, out)
    );
    assertTrue(ex.getMessage().contains("later than"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (1 night)")
  void recalcNights_valid_oneNight() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 3, 1);
    final LocalDate out = LocalDate.of(2025, 3, 2);

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret, "Should return the same Reservations instance");
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
    assertEquals(1, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (cross month, 5 nights)")
  void recalcNights_valid_crossMonth_fiveNights() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 1, 29);
    final LocalDate out = LocalDate.of(2025, 2, 3); // 5 nights

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
    assertEquals(5, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (leap year across Feb 29)")
  void recalcNights_valid_leapYear() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2028, 2, 28);
    final LocalDate out = LocalDate.of(2028, 3, 2); // nights = 3 (28->29, 29->1, 1->2)

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(3, r.getNights());
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → valid range (year boundary, 2 nights)")
  void recalcNights_valid_yearBoundary_twoNights() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 12, 31);
    final LocalDate out = LocalDate.of(2026, 1, 2);

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
    assertEquals(2, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → valid range (30 nights)")
  void recalcNights_valid_longStay_thirtyNights() {
    final Reservations r = new Reservations();
    final LocalDate in = LocalDate.of(2025, 5, 1);
    final LocalDate out = LocalDate.of(2025, 5, 31);

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
    assertEquals(30, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → overwrites existing fields on the same instance")
  void recalcNights_overwritesExistingFields() {
    final Reservations r = new Reservations();
    r.setCheckInDate(LocalDate.of(2025, 1, 1));
    r.setCheckOutDate(LocalDate.of(2025, 1, 3));
    r.setNights(2);

    final LocalDate in = LocalDate.of(2025, 6, 10);
    final LocalDate out = LocalDate.of(2025, 6, 15);

    final Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(in, r.getCheckInDate());
    assertEquals(out, r.getCheckOutDate());
    assertEquals(5, r.getNights());
  }

}
