package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReservationNightsService.
 * Each test explicitly states which method & branch is being tested.
 */
class ReservationNightsServiceTest {

  private final ReservationNightsService service = new ReservationNightsService();

  // -------------------------- recalcNightsOrThrow --------------------------

  @Test
  @DisplayName("recalcNightsOrThrow → branch: null dates (check-in null)")
  void recalcNights_nullCheckIn_throws() {
    // Testing method: recalcNightsOrThrow; branch: parameter validation (check-in is null)
    Reservations r = new Reservations();
    LocalDate out = LocalDate.now().plusDays(1);

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, null, out)
    );
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: null dates (check-out null)")
  void recalcNights_nullCheckOut_throws() {
    // Testing method: recalcNightsOrThrow; branch: parameter validation (check-out is null)
    Reservations r = new Reservations();
    LocalDate in = LocalDate.now();

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, null)
    );
    assertTrue(ex.getMessage().contains("required"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: invalid range (check-out equals check-in)")
  void recalcNights_equalDates_throws() {
    // Testing method: recalcNightsOrThrow; branch: date range invalid (check-out == check-in)
    Reservations r = new Reservations();
    LocalDate in = LocalDate.of(2025, 1, 10);

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, in)
    );
    assertTrue(ex.getMessage().contains("later than"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: invalid range (check-out before check-in)")
  void recalcNights_outBeforeIn_throws() {
    // Testing method: recalcNightsOrThrow; branch: date range invalid (check-out < check-in)
    Reservations r = new Reservations();
    LocalDate in = LocalDate.of(2025, 1, 10);
    LocalDate out = LocalDate.of(2025, 1, 9);

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.recalcNightsOrThrow(r, in, out)
    );
    assertTrue(ex.getMessage().contains("later than"));
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (1 night)")
  void recalcNights_valid_oneNight() {
    // Testing method: recalcNightsOrThrow; branch: happy path (1-night stay)
    Reservations r = new Reservations();
    LocalDate in = LocalDate.of(2025, 3, 1);
    LocalDate out = LocalDate.of(2025, 3, 2);

    Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret, "Should return the same Reservations instance");
    assertEquals(in, r.getCheck_in_date());
    assertEquals(out, r.getCheck_out_date());
    assertEquals(1, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (cross month, 5 nights)")
  void recalcNights_valid_crossMonth_fiveNights() {
    // Testing method: recalcNightsOrThrow; branch: happy path (cross-month multi-night)
    Reservations r = new Reservations();
    LocalDate in = LocalDate.of(2025, 1, 29);
    LocalDate out = LocalDate.of(2025, 2, 3); // 5 nights

    Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(in, r.getCheck_in_date());
    assertEquals(out, r.getCheck_out_date());
    assertEquals(5, r.getNights());
  }

  @Test
  @DisplayName("recalcNightsOrThrow → branch: valid range (leap year across Feb 29)")
  void recalcNights_valid_leapYear() {
    // Testing method: recalcNightsOrThrow; branch: happy path (leap-year boundary)
    // 2028 is a leap year; span includes Feb 29
    Reservations r = new Reservations();
    LocalDate in = LocalDate.of(2028, 2, 28);
    LocalDate out = LocalDate.of(2028, 3, 2); // nights = 3 (28->29, 29->1, 1->2)

    Reservations ret = service.recalcNightsOrThrow(r, in, out);

    assertSame(r, ret);
    assertEquals(3, r.getNights());
    assertEquals(in, r.getCheck_in_date());
    assertEquals(out, r.getCheck_out_date());
  }
}
