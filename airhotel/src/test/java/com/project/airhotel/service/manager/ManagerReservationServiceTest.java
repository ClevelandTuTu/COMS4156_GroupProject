package com.project.airhotel.service.manager;

import com.project.airhotel.dto.reservation.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservation.ReservationUpdateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationInventoryService;
import com.project.airhotel.service.core.ReservationNightsService;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
  ReservationNightsService nightsService;
  @Mock
  ReservationInventoryService inventoryService;
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
  @DisplayName("listReservations → branch: status != null & hasDates")
  void listReservations_statusAndDates() {
    final Long hotelId = 1L;
    final LocalDate s = LocalDate.of(2025, 10, 1);
    final LocalDate e = LocalDate.of(2025, 10, 31);
    final List<Reservations> expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelIdAndStatusAndStayRange(hotelId,
        CONFIRMED, s, e)).thenReturn(expected);

    final List<Reservations> out = service.listReservations(hotelId,
        CONFIRMED, s, e);

    assertEquals(expected, out);
    verify(reservationsRepository, times(1))
        .findByHotelIdAndStatusAndStayRange(hotelId, CONFIRMED, s, e);
  }

  @Test
  @DisplayName("listReservations → branch: hasDates only")
  void listReservations_datesOnly() {
    final Long hotelId = 1L;
    final LocalDate s = LocalDate.of(2025, 10, 1);
    final LocalDate e = LocalDate.of(2025, 10, 31);
    final List<Reservations> expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelIdAndStayRange(hotelId, s, e))
        .thenReturn(expected);

    final List<Reservations> out = service.listReservations(hotelId,
        null, s,
        e);

    assertEquals(expected, out);
    verify(reservationsRepository, times(1))
        .findByHotelIdAndStayRange(hotelId, s, e);
  }

  @Test
  @DisplayName("listReservations → branch: status only")
  void listReservations_statusOnly() {
    final Long hotelId = 1L;
    final List<Reservations> expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelIdAndStatus(hotelId, CHECKED_IN))
        .thenReturn(expected);

    final List<Reservations> out = service.listReservations(hotelId,
        CHECKED_IN, null, null);

    assertEquals(expected, out);
    verify(reservationsRepository, times(1))
        .findByHotelIdAndStatus(hotelId,
        CHECKED_IN);
  }

  @Test
  @DisplayName("listReservations → branch: none (fallback findByHotelId)")
  void listReservations_none() {
    final Long hotelId = 1L;
    final List<Reservations> expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelId(hotelId)).thenReturn(expected);

    final List<Reservations> out = service.listReservations(hotelId,
        null, null, null);

    assertEquals(expected, out);
    verify(reservationsRepository, times(1))
        .findByHotelId(hotelId);
  }

  @Test
  @DisplayName("listReservations → edge: only start date provided (falls back"
      + " to findByHotelId)")
  void listReservations_onlyStartDate() {
    final Long hotelId = 1L;
    final LocalDate s = LocalDate.of(2025, 10, 1);
    final var expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelId(hotelId)).thenReturn(expected);

    final var out = service.listReservations(hotelId, null, s, null);

    assertEquals(expected, out);
    verify(reservationsRepository).findByHotelId(hotelId);
  }

  @Test
  @DisplayName("listReservations → edge: status set but only one date "
      + "provided (falls back to status-only)")
  void listReservations_statusWithSingleDate() {
    final Long hotelId = 1L;
    final LocalDate s = LocalDate.of(2025, 10, 1);
    final var expected = List.of(baseRes());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(reservationsRepository.findByHotelIdAndStatus(hotelId, CHECKED_IN))
        .thenReturn(expected);

    final var out = service.listReservations(hotelId, CHECKED_IN, s, null);

    assertEquals(expected, out);
    verify(reservationsRepository).findByHotelIdAndStatus(hotelId, CHECKED_IN);
  }

  @Test
  @DisplayName("listReservations → invalid: hotel does not exist (guard " +
      "throws)")
  void listReservations_hotelNotFound_throws() {
    final Long hotelId = 99L;
    doThrow(new com.project.airhotel.exception.NotFoundException("hotel not " +
        "found"))
        .when(entityGuards).ensureHotelExists(hotelId);

    assertThrows(com.project.airhotel.exception.NotFoundException.class,
        () -> service.listReservations(hotelId, null, null,
            null));

    verifyNoInteractions(reservationsRepository);
  }


  // ========================== getReservation ==========================

  @Test
  @DisplayName("getReservation → branch: delegates to EntityGuards" +
      ".getReservationInHotelOrThrow")
  void getReservation_delegates() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final Reservations out = service.getReservation(hotelId, resId);

    assertSame(r, out);
    verify(entityGuards, times(1))
        .getReservationInHotelOrThrow(hotelId, resId);
  }

  // ========================== patchReservation ==========================

  @Test
  @DisplayName("patchReservation → branch: change roomType only (release old "
      + "→ set type → reserve old dates)")
  void patchReservation_changeRoomType_only() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getRoomTypeId()).thenReturn(22L);
    when(req.getCheckInDate()).thenReturn(null);
    when(req.getCheckOutDate()).thenReturn(null);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId,
        22L);

    when(reservationsRepository.save(any(Reservations.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final Reservations out = service.patchReservation(hotelId, resId, req);

    assertSame(out, r);
    assertEquals(22L, r.getRoomTypeId());
    verify(inventoryService).releaseRange(1L, 11L,
        r.getCheckInDate(),
        r.getCheckOutDate());
    verify(inventoryService).reserveRangeOrThrow(1L, 22L,
        r.getCheckInDate(),
        r.getCheckOutDate());
    verifyNoInteractions(nightsService, statusService);
  }

  @Test
  @DisplayName("patchReservation → branch: set roomId (ensure belongs to "
      + "hotel & type)")
  void patchReservation_setRoomId() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getRoomTypeId()).thenReturn(null);
    when(req.getRoomId()).thenReturn(555L);

    doNothing().when(entityGuards).ensureRoomBelongsToHotelAndType(hotelId,
        555L, r.getRoomTypeId());
    when(reservationsRepository.save(r)).thenReturn(r);

    final Reservations out = service.patchReservation(hotelId, resId, req);

    assertSame(r, out);
    assertEquals(555L, r.getRoomId());
    verify(entityGuards).ensureRoomBelongsToHotelAndType(hotelId, 555L,
        11L);
    verifyNoInteractions(inventoryService, nightsService, statusService);
  }

  @Test
  @DisplayName("patchReservation → branch: change dates only (release old → " +
      "recalc nights → reserve new)")
  void patchReservation_changeDates_only() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final LocalDate newIn = LocalDate.of(2025, 10, 23);
    final LocalDate newOut = LocalDate.of(2025, 10, 26);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getRoomTypeId()).thenReturn(null);
    when(req.getRoomId()).thenReturn(null);
    when(req.getCheckInDate()).thenReturn(newIn);
    when(req.getCheckOutDate()).thenReturn(newOut);

    when(nightsService.recalcNightsOrThrow(eq(r), eq(newIn), eq(newOut)))
        .thenAnswer(inv -> {
      r.setCheckInDate(newIn);
      r.setCheckOutDate(newOut);
      r.setNights((int) (newOut.toEpochDay() - newIn.toEpochDay()));
      return r;
    });

    when(reservationsRepository.save(r)).thenReturn(r);

    final Reservations out = service.patchReservation(hotelId, resId, req);

    assertSame(r, out);
    assertEquals(3, r.getNights());
    verify(inventoryService).releaseRange(1L, 11L,
        LocalDate.of(2025, 10, 20)
        , LocalDate.of(2025, 10, 22));
    verify(nightsService).recalcNightsOrThrow(r, newIn, newOut);
    verify(inventoryService).reserveRangeOrThrow(1L,
        11L, newIn, newOut);
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("patchReservation → branch: set scalar fields " +
      "(numGuests/currency/priceTotal/notes)")
  void patchReservation_setScalars() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getNumGuests()).thenReturn(4);
    when(req.getCurrency()).thenReturn("EUR");
    when(req.getPriceTotal()).thenReturn(BigDecimal.valueOf(555.55));
    when(req.getNotes()).thenReturn("vip guest");

    when(reservationsRepository.save(r)).thenReturn(r);

    final Reservations out = service.patchReservation(hotelId, resId, req);

    assertSame(r, out);
    assertEquals(4, r.getNumGuests());
    assertEquals("EUR", r.getCurrency());
    assertEquals(BigDecimal.valueOf(555.55), r.getPriceTotal());
    assertEquals("vip guest", r.getNotes());
  }

  @Test
  @DisplayName("patchReservation → branch: status only (delegates to "
      + "statusService and saves returned entity)")
  void patchReservation_statusOnly() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ReservationUpdateRequest req = mock(ReservationUpdateRequest.class);
    when(req.getStatus()).thenReturn(CHECKED_IN);

    final Reservations rAfter = baseRes();
    rAfter.setStatus(CHECKED_IN);
    when(statusService.changeStatus(r, CHECKED_IN, null,
        null)).thenReturn(rAfter);

    when(reservationsRepository.save(rAfter)).thenReturn(rAfter);

    final Reservations out = service.patchReservation(hotelId, resId, req);

    assertSame(rAfter, out);
    verify(statusService, times(1))
        .changeStatus(r, CHECKED_IN, null, null);
  }

  // ========================== applyUpgrade ==========================

  @Test
  @DisplayName("applyUpgrade → branch: invalid upgrade status (neither "
      + "ELIGIBLE nor APPLIED) -> throws")
  void applyUpgrade_invalidStatus_throws() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setUpgradeStatus(UpgradeStatus.NOT_ELIGIBLE);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ApplyUpgradeRequest req = mock(ApplyUpgradeRequest.class);
    when(req.getNewRoomTypeId()).thenReturn(22L);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId,
        22L);

    assertThrows(BadRequestException.class,
        () -> service.applyUpgrade(hotelId, resId, req));
    verify(inventoryService, never()).releaseRange(anyLong(), anyLong(),
        any(), any());
    verify(reservationsRepository, never()).save(any());
  }

  @Test
  @DisplayName("applyUpgrade → branch: happy path (release old → set new type" +
      " → reserve → set upgrade fields → save)")
  void applyUpgrade_happyPath() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final ApplyUpgradeRequest req = mock(ApplyUpgradeRequest.class);
    when(req.getNewRoomTypeId()).thenReturn(22L);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId,
        22L);
    when(reservationsRepository.save(any(Reservations.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final LocalDateTime before = LocalDateTime.now();
    final Reservations out = service.applyUpgrade(hotelId, resId, req);
    final LocalDateTime after = LocalDateTime.now();

    assertSame(r, out);
    assertEquals(22L, r.getRoomTypeId());
    assertEquals(UpgradeStatus.APPLIED, r.getUpgradeStatus());
    assertNotNull(r.getUpgradedAt());
    assertFalse(r.getUpgradedAt().isBefore(before));
    assertFalse(r.getUpgradedAt().isAfter(after));

    verify(inventoryService).releaseRange(1L, 11L,
        LocalDate.of(2025, 10, 20)
        , LocalDate.of(2025, 10, 22));
    verify(inventoryService).reserveRangeOrThrow(1L, 22L,
        LocalDate.of(2025, 10, 20),
        LocalDate.of(2025, 10, 22));
    verify(reservationsRepository, times(1)).save(r);
  }

  @Test
  @DisplayName("applyUpgrade → edge valid: already APPLIED is allowed "
      + "(re-applies and timestamps)")
  void applyUpgrade_alreadyApplied_allowed() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setUpgradeStatus(UpgradeStatus.APPLIED);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final var req = mock(ApplyUpgradeRequest.class);
    when(req.getNewRoomTypeId()).thenReturn(33L);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId,
        33L);
    when(reservationsRepository.save(any(Reservations.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final var before = LocalDateTime.now();
    final Reservations out = service.applyUpgrade(hotelId, resId, req);
    final var after = LocalDateTime.now();

    assertSame(r, out);
    assertEquals(33L, r.getRoomTypeId());
    assertEquals(UpgradeStatus.APPLIED, r.getUpgradeStatus());
    assertNotNull(r.getUpgradedAt());
    assertFalse(r.getUpgradedAt().isBefore(before));
    assertFalse(r.getUpgradedAt().isAfter(after));

    verify(inventoryService).releaseRange(1L, 11L,
        r.getCheckInDate(),
        r.getCheckOutDate());
    verify(inventoryService).reserveRangeOrThrow(1L, 33L,
        r.getCheckInDate(),
        r.getCheckOutDate());
    verify(reservationsRepository).save(r);
  }


  // ========================== checkIn ==========================

  @Test
  @DisplayName("checkIn → branch: status == CANCELED (throws " +
      "BadRequestException)")
  void checkIn_canceled_throws() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setStatus(CANCELED);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    assertThrows(BadRequestException.class, () -> service.checkIn(hotelId,
        resId));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkIn → branch: already CHECKED_IN returns as-is")
  void checkIn_alreadyCheckedIn_returns() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setStatus(CHECKED_IN);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final Reservations out = service.checkIn(hotelId, resId);

    assertSame(r, out);
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkIn → branch: transition to CHECKED_IN via statusService")
  void checkIn_transition() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes(); // PENDING
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final Reservations r2 = baseRes();
    r2.setStatus(CHECKED_IN);
    when(statusService.changeStatus(r, CHECKED_IN, null,
        null))
        .thenReturn(r2);

    final Reservations out = service.checkIn(hotelId, resId);

    assertSame(r2, out);
    verify(statusService, times(1)).changeStatus(r,
        CHECKED_IN, null, null);
  }

  // ========================== checkOut ==========================

  @Test
  @DisplayName("checkOut → branch: status == CANCELED (throws " +
      "BadRequestException)")
  void checkOut_canceled_throws() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setStatus(CANCELED);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    assertThrows(BadRequestException.class, () -> service.checkOut(hotelId,
        resId));
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkOut → branch: already CHECKED_OUT returns as-is")
  void checkOut_alreadyCheckedOut_returns() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    r.setStatus(CHECKED_OUT);
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final Reservations out = service.checkOut(hotelId, resId);

    assertSame(r, out);
    verifyNoInteractions(statusService);
  }

  @Test
  @DisplayName("checkOut → branch: transition to CHECKED_OUT via statusService")
  void checkOut_transition() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    final Reservations r2 = baseRes();
    r2.setStatus(CHECKED_OUT);
    when(statusService.changeStatus(r, CHECKED_OUT, null,
        null)).thenReturn(r2);

    final Reservations out = service.checkOut(hotelId, resId);

    assertSame(r2, out);
    verify(statusService, times(1)).changeStatus(r,
        CHECKED_OUT, null, null);
  }

  // ========================== cancel ==========================

  @Test
  @DisplayName("cancel → branch: delegates to orchestrator.cancel")
  void cancel_delegates() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    doNothing().when(orchestrator).cancel(r, "mgr-cancel",
        null);

    service.cancel(hotelId, resId, "mgr-cancel");

    verify(orchestrator, times(1)).cancel(r,
        "mgr-cancel", null);
  }

  @Test
  @DisplayName("cancel → edge valid: null reason is accepted and forwarded")
  void cancel_reasonNull() {
    final Long hotelId = 1L;
    final Long resId = 100L;
    final Reservations r = baseRes();
    when(entityGuards.getReservationInHotelOrThrow(hotelId, resId))
        .thenReturn(r);

    doNothing().when(orchestrator).cancel(r, null, null);

    service.cancel(hotelId, resId, null);

    verify(orchestrator).cancel(r, null, null);
  }

  @Test
  @DisplayName("cancel → invalid: guard throws (reservation not found)")
  void cancel_guardThrows() {
    final Long hotelId = 1L;
    final Long resId = 404L;
    doThrow(new com.project.airhotel.exception.NotFoundException("not found"))
        .when(entityGuards).getReservationInHotelOrThrow(hotelId, resId);

    assertThrows(com.project.airhotel.exception.NotFoundException.class,
        () -> service.cancel(hotelId, resId, "any"));

    verifyNoInteractions(orchestrator);
  }

}
