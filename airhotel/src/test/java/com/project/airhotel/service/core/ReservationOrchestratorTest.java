package com.project.airhotel.service.core;

import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.reservation.policy.ReservationChangePolicy;
import com.project.airhotel.reservation.service.ReservationInventoryService;
import com.project.airhotel.reservation.service.ReservationNightsService;
import com.project.airhotel.reservation.service.ReservationOrchestrator;
import com.project.airhotel.reservation.service.ReservationPricingService;
import com.project.airhotel.reservation.service.ReservationStatusService;
import com.project.airhotel.room.repository.RoomTypeDailyPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.project.airhotel.reservation.domain.enums.ReservationStatus.CANCELED;
import static com.project.airhotel.reservation.domain.enums.ReservationStatus.CHECKED_OUT;
import static com.project.airhotel.reservation.domain.enums.ReservationStatus.CONFIRMED;
import static com.project.airhotel.reservation.domain.enums.ReservationStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReservationOrchestrator after refactor. Cover:
 * modifyReservation (dates/type/scalars/status), createReservation, cancel.
 */
@ExtendWith(MockitoExtension.class)
class ReservationOrchestratorTest {

  @Mock
  ReservationInventoryService inventoryService;
  @Mock
  ReservationStatusService statusService;
  @Mock
  ReservationNightsService nightsService;
  @Mock
  EntityGuards entityGuards;
  @Mock
  ReservationsRepository reservationsRepository;
  @Mock
  ReservationPricingService pricingService;

  @InjectMocks
  ReservationOrchestrator orchestrator;

  private Reservations baseReservation(final ReservationStatus status) {
    final Reservations r = new Reservations();
    r.setId(100L);
    r.setHotelId(1L);
    r.setRoomTypeId(11L);
    r.setStatus(status);
    r.setCheckInDate(LocalDate.of(2025, 10, 20));
    r.setCheckOutDate(LocalDate.of(2025, 10, 22));
    return r;
  }

  // ===================== createReservation =====================

  @Test
  @DisplayName("createReservation → happy path: guards ok, nights set, " +
      "inventory applied, saved")
  void createReservation_happy() {
    final Long userId = 9L;
    final LocalDate in = LocalDate.of(2025, 10, 20);
    final LocalDate out = LocalDate.of(2025, 10, 22);

    final CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(2);
    when(req.getCurrency()).thenReturn(null); // default USD
    when(req.getPriceTotal()).thenReturn(null);
    when(req.getCheckInDate()).thenReturn(in);
    when(req.getCheckOutDate()).thenReturn(out);

    // guards pass
    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    // nights service must WRITE BACK dates and nights to entity
    Mockito.lenient().doAnswer(inv -> {
      final Reservations r = inv.getArgument(0);
      final LocalDate ci = inv.getArgument(1);
      final LocalDate co = inv.getArgument(2);
      r.setCheckInDate(ci);
      r.setCheckOutDate(co);
      r.setNights((int) (co.toEpochDay() - ci.toEpochDay()));
      return r;
    }).when(nightsService).recalcNightsOrThrow(any(Reservations.class),
        eq(in), eq(out));

    final BigDecimal expectedTotal = new BigDecimal("3200.00");
    Mockito.lenient().doAnswer(inv -> {
      final Reservations r = inv.getArgument(0);
      r.setPriceTotal(expectedTotal);
      return null;
    }).when(pricingService).recalcTotalPriceOrThrow(any(Reservations.class));

    when(reservationsRepository.save(any(Reservations.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final Reservations saved = orchestrator.createReservation(userId, req);

    assertEquals(userId, saved.getUserId());
    assertEquals(1L, saved.getHotelId());
    assertEquals(11L, saved.getRoomTypeId());
    assertEquals("USD", saved.getCurrency());
    assertEquals(expectedTotal, saved.getPriceTotal());
    assertEquals(in, saved.getCheckInDate());
    assertEquals(out, saved.getCheckOutDate());
    assertEquals(2, saved.getNights());

    verify(nightsService).recalcNightsOrThrow(any(Reservations.class), eq(in), eq(out));
    verify(pricingService).recalcTotalPriceOrThrow(any(Reservations.class));
    verify(inventoryService).applyRangeChangeOrThrow(1L, null, null, null,
        11L, in, out);
  }

  @Test
  @DisplayName("createReservation → validations: non-positive guests and " +
      "negative price")
  void createReservation_validations() {
    final CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(0);

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    assertThrows(BadRequestException.class,
        () -> orchestrator.createReservation(9L, req));

    // negative price
    when(req.getNumGuests()).thenReturn(2);
    when(req.getPriceTotal()).thenReturn(new BigDecimal("-0.01"));
    assertThrows(BadRequestException.class,
        () -> orchestrator.createReservation(9L, req));

    verifyNoInteractions(inventoryService);
  }

  // ===================== cancel =====================

  @Test
  @DisplayName("cancel → already CANCELED is a no-op, no side effects")
  void cancel_alreadyCanceled_noop() {
    final Reservations r = baseReservation(CANCELED);

    orchestrator.cancel(r, "x", 9L);

    verifyNoInteractions(inventoryService, statusService);
    assertNull(r.getCanceledAt());
  }

  @Test
  @DisplayName("cancel → CHECKED_OUT throws")
  void cancel_checkedOut_throws() {
    final Reservations r = baseReservation(CHECKED_OUT);
    assertThrows(BadRequestException.class, () -> orchestrator.cancel(r, "ops"
        , 8L));
    verifyNoInteractions(inventoryService, statusService);
    assertNull(r.getCanceledAt());
  }

  @Test
  @DisplayName("cancel → happy path: unified inventory apply, timestamp set, " +
      "status change")
  void cancel_happy() {
    final Reservations r = baseReservation(CONFIRMED);

    final LocalDateTime before = LocalDateTime.now();
    orchestrator.cancel(r, "user", 7L);
    final LocalDateTime after = LocalDateTime.now();

    verify(inventoryService).applyRangeChangeOrThrow(
        r.getHotelId(),
        r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate(),
        null, null, null
    );
    assertNotNull(r.getCanceledAt());
    assertFalse(r.getCanceledAt().isBefore(before));
    assertFalse(r.getCanceledAt().isAfter(after));

    verify(statusService).changeStatus(r, CANCELED, "user", 7L);

    final InOrder inOrder = inOrder(inventoryService, statusService);
    inOrder.verify(inventoryService).applyRangeChangeOrThrow(any(), any(),
        any(), any(), any(), any(), any());
    inOrder.verify(statusService).changeStatus(any(), any(), any(), any());
  }

  @Test
  @DisplayName("cancel → if statusService throws, inventory already applied " +
      "and timestamp set; exception bubbles")
  void cancel_statusThrows() {
    final Reservations r = baseReservation(CONFIRMED);
    doThrow(new RuntimeException("boom")).when(statusService).changeStatus(eq(r), eq(CANCELED), any(), any());
    assertThrows(RuntimeException.class, () -> orchestrator.cancel(r, "r", 1L));
    verify(inventoryService).applyRangeChangeOrThrow(any(), any(), any(),
        any(), any(), any(), any());
    assertNotNull(r.getCanceledAt());
  }

  // ===================== modifyReservation =====================

  private ReservationChangePolicy managerPolicyAllowAll() {
    return new ReservationChangePolicy() {
      @Override
      public boolean allowChangeRoomType() {
        return true;
      }

      @Override
      public boolean allowAssignConcreteRoom() {
        return true;
      }

      @Override
      public boolean allowStatusChangeTo(final ReservationStatus to) {
        return true;
      }
    };
  }

  @Test
  @DisplayName("modifyReservation → dates + roomType change: nights " +
      "recalculated, unified inventory apply, saved")
  void modifyReservation_datesAndType() {
    final Reservations r = baseReservation(PENDING);

    // boundary & existence
    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 22L);

    final LocalDate newIn = LocalDate.of(2025, 10, 23);
    final LocalDate newOut = LocalDate.of(2025, 10, 26);

    // nights write-back
    Mockito.lenient().doAnswer(inv -> {
      final Reservations rr = inv.getArgument(0);
      rr.setCheckInDate(newIn);
      rr.setCheckOutDate(newOut);
      rr.setNights((int) (newOut.toEpochDay() - newIn.toEpochDay()));
      return rr;
    }).when(nightsService).recalcNightsOrThrow(eq(r), eq(newIn), eq(newOut));

    when(reservationsRepository.save(r)).thenAnswer(inv -> inv.getArgument(0));

    final ReservationChange change = ReservationChange.builder()
        .newRoomTypeId(22L)
        .newCheckIn(newIn)
        .newCheckOut(newOut)
        .build();

    final Reservations out = orchestrator.modifyReservation(1L, r, change,
        managerPolicyAllowAll());

    assertSame(r, out);
    assertEquals(22L, r.getRoomTypeId());
    assertEquals(3, r.getNights());

    verify(inventoryService).applyRangeChangeOrThrow(
        eq(1L),
        eq(11L), eq(LocalDate.of(2025, 10, 20)), eq(LocalDate.of(2025, 10, 22)),
        eq(22L), eq(newIn), eq(newOut)
    );
    verify(pricingService).recalcTotalPriceOrThrow(r);
    verify(reservationsRepository).save(r);
  }

  @Test
  @DisplayName("modifyReservation → assign concrete room requires guard and " +
      "sets roomId")
  void modifyReservation_assignRoom() {
    final Reservations r = baseReservation(PENDING);
    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomBelongsToHotelAndType(1L, 555L,
        11L);
    when(reservationsRepository.save(r)).thenAnswer(inv -> inv.getArgument(0));

    final ReservationChange change = ReservationChange.builder()
        .newRoomId(555L)
        .build();

    final Reservations out = orchestrator.modifyReservation(1L, r, change,
        managerPolicyAllowAll());
    assertSame(r, out);
    assertEquals(555L, r.getRoomId());
    verify(entityGuards).ensureRoomBelongsToHotelAndType(1L, 555L, 11L);
  }

  @Test
  @DisplayName("modifyReservation → invalid guests and negative price both " +
      "throw")
  void modifyReservation_invalidScalars_throw() {
    final Reservations r = baseReservation(PENDING);
    doNothing().when(entityGuards).ensureHotelExists(1L);

    // invalid guests
    final ReservationChange badGuests =
        ReservationChange.builder().newNumGuests(0).build();
    assertThrows(BadRequestException.class,
        () -> orchestrator.modifyReservation(1L, r, badGuests,
            managerPolicyAllowAll()));
    verifyNoInteractions(inventoryService, statusService);

    // negative price
    final ReservationChange badPrice =
        ReservationChange.builder().newPriceTotal(new BigDecimal("-1")).build();
    assertThrows(BadRequestException.class,
        () -> orchestrator.modifyReservation(1L, r, badPrice,
            managerPolicyAllowAll()));
  }

  @Test
  @DisplayName("modifyReservation → status change delegates to statusService")
  void modifyReservation_statusChange() {
    final Reservations r = baseReservation(PENDING);
    doNothing().when(entityGuards).ensureHotelExists(1L);
    when(reservationsRepository.save(r)).thenAnswer(inv -> inv.getArgument(0));

    final ReservationChange change =
        ReservationChange.builder().newStatus(CONFIRMED).build();
    final Reservations out = orchestrator.modifyReservation(1L, r, change,
        managerPolicyAllowAll());

    assertSame(r, out);
    verify(statusService).changeStatus(r, CONFIRMED, null, null);
  }

  @Test
  @DisplayName("modifyReservation → invalid effective dates throw")
  void modifyReservation_invalidDates_throw() {
    final Reservations r = baseReservation(PENDING);
    doNothing().when(entityGuards).ensureHotelExists(1L);

    final LocalDate in = LocalDate.of(2025, 10, 20);
    final LocalDate out = LocalDate.of(2025, 10, 20); // not after
    final ReservationChange change =
        ReservationChange.builder().newCheckIn(in).newCheckOut(out).build();

    assertThrows(BadRequestException.class,
        () -> orchestrator.modifyReservation(1L, r, change,
            managerPolicyAllowAll()));
  }

  @Test
  @DisplayName("modifyReservation → wrong hotel ownership throws")
  void modifyReservation_wrongHotel_throw() {
    final Reservations r = baseReservation(PENDING);
    r.setHotelId(2L); // mismatched

    doNothing().when(entityGuards).ensureHotelExists(1L);

    final ReservationChange change = ReservationChange.builder().build();
    assertThrows(BadRequestException.class,
        () -> orchestrator.modifyReservation(1L, r, change,
            managerPolicyAllowAll()));
  }
}
