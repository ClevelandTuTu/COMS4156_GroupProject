package com.project.airhotel.service.manager;

import com.project.airhotel.dto.reservation.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservation.ReservationUpdateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationOrchestrator;
import com.project.airhotel.service.core.ReservationStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.project.airhotel.model.enums.ReservationStatus.CANCELED;
import static com.project.airhotel.model.enums.ReservationStatus.CHECKED_IN;
import static com.project.airhotel.model.enums.ReservationStatus.CHECKED_OUT;
import static com.project.airhotel.model.enums.ReservationStatus.CONFIRMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ManagerReservationService. Each test states which method &
 * which branch is being exercised.
 */
@ExtendWith(MockitoExtension.class)
class ManagerReservationServiceTest {

  @Mock
  ReservationsRepository reservationsRepository;
  @Mock
  EntityGuards entityGuards;
  @Mock
  ReservationStatusService statusService;
  @Mock
  ReservationOrchestrator orchestrator;

  @InjectMocks
  ManagerReservationService service;

  private Reservations baseRes() {
    final Reservations r = new Reservations();
    r.setId(100L);
    r.setHotelId(1L);
    r.setRoomTypeId(11L);
    r.setRoomId(0L);
    r.setStatus(ReservationStatus.PENDING);
    r.setUpgradeStatus(UpgradeStatus.ELIGIBLE);
    r.setCheckInDate(LocalDate.of(2025, 10, 20));
    r.setCheckOutDate(LocalDate.of(2025, 10, 22));
    r.setNights(2);
    r.setCurrency("USD");
    r.setPriceTotal(BigDecimal.valueOf(200));
    r.setNumGuests(2);
    return r;
  }

  // ========================== listReservations ==========================

  @Test
  @DisplayName("listReservations → status + dates")
  void listReservations_statusAndDates() {
    final var expected = List.of(baseRes());
    doNothing().when(entityGuards).ensureHotelExists(1L);
    when(reservationsRepository.findByHotelIdAndStatusAndStayRange(1L, CONFIRMED,
        LocalDate.of(2025,10,1), LocalDate.of(2025,10,31))).thenReturn(expected);

    final var out = service.listReservations(1L, CONFIRMED,
        LocalDate.of(2025,10,1), LocalDate.of(2025,10,31));

    assertEquals(expected, out);
  }

  @Test
  @DisplayName("listReservations → dates only")
  void listReservations_datesOnly() {
    final var expected = List.of(baseRes());
    doNothing().when(entityGuards).ensureHotelExists(1L);
    when(reservationsRepository.findByHotelIdAndStayRange(1L,
        LocalDate.of(2025,10,1), LocalDate.of(2025,10,31))).thenReturn(expected);

    final var out = service.listReservations(1L, null,
        LocalDate.of(2025,10,1), LocalDate.of(2025,10,31));

    assertEquals(expected, out);
  }

  @Test
  @DisplayName("listReservations → status only")
  void listReservations_statusOnly() {
    final var expected = List.of(baseRes());
    doNothing().when(entityGuards).ensureHotelExists(1L);
    when(reservationsRepository.findByHotelIdAndStatus(1L, CHECKED_IN))
        .thenReturn(expected);

    final var out = service.listReservations(1L, CHECKED_IN, null, null);

    assertEquals(expected, out);
  }

  @Test
  @DisplayName("listReservations → none (fallback)")
  void listReservations_none() {
    final var expected = List.of(baseRes());
    doNothing().when(entityGuards).ensureHotelExists(1L);
    when(reservationsRepository.findByHotelId(1L)).thenReturn(expected);

    final var out = service.listReservations(1L, null, null, null);

    assertEquals(expected, out);
  }

  @Test
  @DisplayName("listReservations → hotel not found (guard throws)")
  void listReservations_hotelNotFound() {
    doThrow(new com.project.airhotel.exception.NotFoundException("hotel not found"))
        .when(entityGuards).ensureHotelExists(99L);

    assertThrows(com.project.airhotel.exception.NotFoundException.class,
        () -> service.listReservations(99L, null, null, null));

    verifyNoInteractions(reservationsRepository);
  }

  // ========================== getReservation ==========================

  @Test
  @DisplayName("getReservation → delegates to guards.getReservationInHotelOrThrow")
  void getReservation_delegates() {
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    assertSame(r, service.getReservation(1L, 100L));
  }

  // ========================== patchReservation ==========================

  @Test
  @DisplayName("patchReservation → delegates to orchestrator.modifyReservation (dates change)")
  void patchReservation_delegates_dates() {
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getCheckInDate()).thenReturn(LocalDate.of(2025,10,23));
    when(req.getCheckOutDate()).thenReturn(LocalDate.of(2025,10,25));

    final Reservations updated = baseRes();
    updated.setCheckInDate(LocalDate.of(2025,10,23));
    updated.setCheckOutDate(LocalDate.of(2025,10,25));

    when(orchestrator.modifyReservation(eq(1L), same(r), any(), any())).thenReturn(updated);

    final Reservations out = service.patchReservation(1L, 100L, req);

    assertSame(updated, out);
    verify(orchestrator).modifyReservation(eq(1L), same(r), argThat(ch ->
        ch.newCheckIn() != null && ch.newCheckOut() != null
    ), any());
  }

  @Test
  @DisplayName("patchReservation → status only → orchestrator invoked with newStatus")
  void patchReservation_statusOnly() {
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getStatus()).thenReturn(CHECKED_IN);

    final Reservations rAfter = baseRes(); rAfter.setStatus(CHECKED_IN);
    when(orchestrator.modifyReservation(eq(1L), same(r), any(), any())).thenReturn(rAfter);

    final Reservations out = service.patchReservation(1L, 100L, req);
    assertSame(rAfter, out);

    verify(orchestrator).modifyReservation(eq(1L), same(r), argThat(ch ->
        ch.newStatus() == CHECKED_IN
    ), any());
  }

  // ========================== applyUpgrade ==========================

  @Test
  @DisplayName("applyUpgrade → invalid upgrade status → throws")
  void applyUpgrade_invalidStatus() {
    final Reservations r = baseRes();
    r.setUpgradeStatus(UpgradeStatus.NOT_ELIGIBLE);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);

    final ApplyUpgradeRequest req = mock(ApplyUpgradeRequest.class);
    when(req.getNewRoomTypeId()).thenReturn(22L);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 22L);

    assertThrows(BadRequestException.class, () -> service.applyUpgrade(1L, 100L, req));
    verify(orchestrator, never()).modifyReservation(anyLong(), any(), any(), any());
  }

  @Test
  @DisplayName("applyUpgrade → happy path: ensureRoomType → orchestrator.modifyReservation → set APPLIED & timestamp → save")
  void applyUpgrade_happyPath() {
    final Reservations r = baseRes(); // ELIGIBLE
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);

    final ApplyUpgradeRequest req = mock(ApplyUpgradeRequest.class);
    when(req.getNewRoomTypeId()).thenReturn(22L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 22L);

    final Reservations updated = baseRes();
    updated.setRoomTypeId(22L);
    when(orchestrator.modifyReservation(eq(1L), same(r), any(), any())).thenReturn(updated);

    when(reservationsRepository.save(updated)).thenAnswer(inv -> inv.getArgument(0));

    final LocalDateTime before = LocalDateTime.now();
    final Reservations out = service.applyUpgrade(1L, 100L, req);
    final LocalDateTime after = LocalDateTime.now();

    assertSame(updated, out);
    assertEquals(UpgradeStatus.APPLIED, out.getUpgradeStatus());
    assertNotNull(out.getUpgradedAt());
    assertFalse(out.getUpgradedAt().isBefore(before));
    assertFalse(out.getUpgradedAt().isAfter(after));

    verify(orchestrator).modifyReservation(eq(1L), same(r), argThat(ch ->
        ch.newRoomTypeId() != null && ch.newRoomTypeId() == 22L
    ), any());
    verify(reservationsRepository).save(updated);
  }

  // ========================== checkIn / checkOut ==========================

  @Test
  @DisplayName("checkIn → canceled → throws BadRequestException")
  void checkIn_canceled_throws() {
    final Reservations r = baseRes(); r.setStatus(CANCELED);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    assertThrows(BadRequestException.class, () -> service.checkIn(1L, 100L));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkIn → already CHECKED_IN → returns as-is")
  void checkIn_already() {
    final Reservations r = baseRes(); r.setStatus(CHECKED_IN);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    assertSame(r, service.checkIn(1L, 100L));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkIn → transitions via statusService")
  void checkIn_transition() {
    final Reservations r = baseRes(); // PENDING
    final Reservations r2 = baseRes(); r2.setStatus(CHECKED_IN);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    when(statusService.changeStatus(r, CHECKED_IN, null, null)).thenReturn(r2);

    final Reservations out = service.checkIn(1L, 100L);
    assertSame(r2, out);
    verify(statusService).changeStatus(r, CHECKED_IN, null, null);
  }

  @Test
  @DisplayName("checkOut → canceled → throws BadRequestException")
  void checkOut_canceled_throws() {
    final Reservations r = baseRes(); r.setStatus(CANCELED);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    assertThrows(BadRequestException.class, () -> service.checkOut(1L, 100L));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkOut → already CHECKED_OUT → returns as-is")
  void checkOut_already() {
    final Reservations r = baseRes(); r.setStatus(CHECKED_OUT);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    assertSame(r, service.checkOut(1L, 100L));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkOut → transitions via statusService")
  void checkOut_transition() {
    final Reservations r = baseRes();
    final Reservations r2 = baseRes(); r2.setStatus(CHECKED_OUT);
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);
    when(statusService.changeStatus(r, CHECKED_OUT, null, null)).thenReturn(r2);

    final Reservations out = service.checkOut(1L, 100L);
    assertSame(r2, out);
    verify(statusService).changeStatus(r, CHECKED_OUT, null, null);
  }

  // ========================== cancel ==========================

  @Test
  @DisplayName("cancel → delegates to orchestrator.cancel (reason passthrough)")
  void cancel_delegates() {
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(r);

    doNothing().when(orchestrator).cancel(r, "mgr-cancel", null);

    service.cancel(1L, 100L, "mgr-cancel");

    verify(orchestrator).cancel(r, "mgr-cancel", null);
  }
}
