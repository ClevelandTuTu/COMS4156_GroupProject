package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.project.airhotel.model.enums.ReservationStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationOrchestrator.
 * Each test clearly states which method & branch is exercised.
 */
@ExtendWith(MockitoExtension.class)
class ReservationOrchestratorTest {

  @Mock
  private ReservationInventoryService inventoryService;

  @Mock
  private ReservationStatusService statusService;

  @InjectMocks
  private ReservationOrchestrator orchestrator;

  private Reservations baseReservation(ReservationStatus status) {
    Reservations r = new Reservations();
    r.setId(100L);
    r.setStatus(status);
    r.setHotelId(1L);
    r.setRoomTypeId(11L);
    r.setCheckInDate(LocalDate.of(2025, 10, 20));
    r.setCheckOutDate(LocalDate.of(2025, 10, 22));
    // r.setCanceled_at(null) by default
    return r;
  }

  @Test
  @DisplayName("cancel → branch: status == CANCELED (early return, no side effects)")
  void cancel_alreadyCanceled_noop() {
    // Testing method: cancel; branch: r.status == CANCELED → return immediately
    Reservations r = baseReservation(CANCELED);

    orchestrator.cancel(r, "any-reason", 9L);

    // No inventory release, no status change, canceled_at unchanged (null)
    verifyNoInteractions(inventoryService, statusService);
    assertNull(r.getCanceledAt(), "canceled_at should remain null on early return");
  }

  @Test
  @DisplayName("cancel → branch: status == CHECKED_OUT (throws BadRequestException)")
  void cancel_checkedOut_throws() {
    // Testing method: cancel; branch: r.status == CHECKED_OUT → throw
    Reservations r = baseReservation(CHECKED_OUT);

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> orchestrator.cancel(r, "ops", 8L)
    );
    assertTrue(ex.getMessage().contains("cannot be cancelled"), "exception message should indicate cannot cancel");

    verifyNoInteractions(inventoryService, statusService);
    assertNull(r.getCanceledAt(), "canceled_at should not be set when exception occurs");
  }

  @ParameterizedTest
  @EnumSource(value = ReservationStatus.class, names = {"PENDING", "CONFIRMED", "CHECKED_IN"})
  @DisplayName("cancel → branch: cancelable statuses (PENDING/CONFIRMED/CHECKED_IN) - happy path")
  void cancel_happyPath_cancelableStatuses(ReservationStatus initialStatus) {
    // Testing method: cancel; branch: happy path for cancelable statuses
    Reservations r = baseReservation(initialStatus);
    String reason = "user-request";
    Long changer = 7L;

    // Time window to assert canceled_at is set "around now"
    LocalDateTime before = LocalDateTime.now();
    orchestrator.cancel(r, reason, changer);
    LocalDateTime after = LocalDateTime.now();

    // 1) inventory released with exact parameters
    verify(inventoryService, times(1))
        .releaseRange(r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());

    // 2) canceled_at set within [before, after]
    assertNotNull(r.getCanceledAt(), "canceled_at should be set");
    assertFalse(r.getCanceledAt().isBefore(before), "canceled_at should not be before 'before'");
    assertFalse(r.getCanceledAt().isAfter(after), "canceled_at should not be after 'after'");

    // 3) status change invoked to CANCELED with reason & user id
    verify(statusService, times(1))
        .changeStatus(r, CANCELED, reason, changer);

    // no further interactions
    verifyNoMoreInteractions(inventoryService, statusService);
  }
}
