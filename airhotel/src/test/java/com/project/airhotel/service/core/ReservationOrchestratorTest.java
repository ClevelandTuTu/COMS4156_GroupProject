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
import org.mockito.InOrder;

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

  private Reservations baseReservation(final ReservationStatus status) {
    final Reservations r = new Reservations();
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
    final Reservations r = baseReservation(CANCELED);

    orchestrator.cancel(r, "any-reason", 9L);

    verifyNoInteractions(inventoryService, statusService);
    assertNull(r.getCanceledAt(), "canceled_at should remain null on early return");
  }

  @Test
  @DisplayName("cancel → branch: status == CHECKED_OUT (throws BadRequestException)")
  void cancel_checkedOut_throws() {
    final Reservations r = baseReservation(CHECKED_OUT);

    final BadRequestException ex = assertThrows(
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
  void cancel_happyPath_cancelableStatuses(final ReservationStatus initialStatus) {
    final Reservations r = baseReservation(initialStatus);
    final String reason = "user-request";
    final Long changer = 7L;

    final LocalDateTime before = LocalDateTime.now();
    orchestrator.cancel(r, reason, changer);
    final LocalDateTime after = LocalDateTime.now();

    verify(inventoryService, times(1))
        .releaseRange(r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());

    assertNotNull(r.getCanceledAt(), "canceled_at should be set");
    assertFalse(r.getCanceledAt().isBefore(before), "canceled_at should not be before 'before'");
    assertFalse(r.getCanceledAt().isAfter(after), "canceled_at should not be after 'after'");

    verify(statusService, times(1))
        .changeStatus(r, CANCELED, reason, changer);

    verifyNoMoreInteractions(inventoryService, statusService);
  }

  @Test
  @DisplayName("cancel → null reason and null userId")
  void cancel_nullReasonNullUser() {
    final Reservations r = baseReservation(ReservationStatus.CONFIRMED);

    orchestrator.cancel(r, null, null);

    verify(inventoryService, times(1))
        .releaseRange(r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());
    verify(statusService, times(1))
        .changeStatus(r, CANCELED, null, null);
    assertNotNull(r.getCanceledAt());
  }

  @Test
  @DisplayName("cancel → operations occur in order: releaseRange then changeStatus")
  void cancel_order_release_then_status() {
    final Reservations r = baseReservation(ReservationStatus.PENDING);
    final String reason = "seq";
    final Long changer = 42L;

    orchestrator.cancel(r, reason, changer);

    final InOrder inOrder = inOrder(inventoryService, statusService);
    inOrder.verify(inventoryService).releaseRange(r.getHotelId(), r.getRoomTypeId(),
        r.getCheckInDate(), r.getCheckOutDate());
    inOrder.verify(statusService).changeStatus(r, CANCELED, reason, changer);
  }

  @Test
  @DisplayName("cancel → changeStatus throws; inventory released, canceledAt set, exception propagated")
  void cancel_changeStatusThrows_propagates() {
    final Reservations r = baseReservation(ReservationStatus.CONFIRMED);
    final String reason = "x";
    final Long changer = 5L;

    doThrow(new RuntimeException("boom")).when(statusService)
        .changeStatus(eq(r), eq(CANCELED), eq(reason), eq(changer));

    assertThrows(RuntimeException.class, () -> orchestrator.cancel(r, reason, changer));

    verify(inventoryService, times(1))
        .releaseRange(r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());
    assertNotNull(r.getCanceledAt());
  }

  @Test
  @DisplayName("cancel → inventory release throws; canceledAt not set and no status change")
  void cancel_inventoryThrows_before() {
    final Reservations r = baseReservation(ReservationStatus.CONFIRMED);

    doThrow(new RuntimeException("inv-err")).when(inventoryService)
        .releaseRange(r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());

    assertThrows(RuntimeException.class, () -> orchestrator.cancel(r, "r", 1L));

    verify(statusService, never()).changeStatus(any(), any(), any(), any());
    assertNull(r.getCanceledAt());
  }
}
