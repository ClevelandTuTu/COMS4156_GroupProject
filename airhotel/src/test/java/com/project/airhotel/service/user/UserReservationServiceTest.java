package com.project.airhotel.service.user;

import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.exception.NotFoundException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.mapper.ReservationMapper;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationInventoryService;
import com.project.airhotel.service.core.ReservationNightsService;
import com.project.airhotel.service.core.ReservationOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserReservationService.
 * Each test states which method & which branch is being exercised.
 */
@ExtendWith(MockitoExtension.class)
class UserReservationServiceTest {

  @Mock private ReservationsRepository reservationsRepository;
  @Mock private ReservationNightsService nightsService;
  @Mock private ReservationInventoryService inventoryService;
  @Mock private ReservationOrchestrator orchestrator;
  @Mock private ReservationMapper mapper;
  @Mock private EntityGuards entityGuards;

  @InjectMocks
  private UserReservationService service;

  // ---------- Helpers ----------
  private Reservations baseReservation() {
    Reservations r = new Reservations();
    r.setId(100L);
    r.setUserId(9L);
    r.setHotelId(1L);
    r.setRoomTypeId(11L);
    r.setNumGuests(2);
    r.setCurrency("USD");
    r.setCheckInDate(LocalDate.of(2025, 10, 20));
    r.setCheckOutDate(LocalDate.of(2025, 10, 22));
    r.setNights(2);
    return r;
  }

  // ========================= listMyReservations =========================

  @Test
  @DisplayName("listMyReservations → branch: repository returns empty list")
  void listMyReservations_empty() {
    // Testing method: listMyReservations; branch: empty result mapping
    Long userId = 9L;
    when(reservationsRepository.findByUserId(userId)).thenReturn(List.of());

    List<ReservationSummaryResponse> out = service.listMyReservations(userId);

    assertNotNull(out);
    assertTrue(out.isEmpty());
    verify(mapper, never()).toSummary(any());
  }

  @Test
  @DisplayName("listMyReservations → branch: repository returns non-empty list (mapping order)")
  void listMyReservations_nonEmpty() {
    // Testing method: listMyReservations; branch: non-empty mapping
    Long userId = 9L;
    Reservations r1 = new Reservations(); r1.setId(1L);
    Reservations r2 = new Reservations(); r2.setId(2L);
    when(reservationsRepository.findByUserId(userId)).thenReturn(List.of(r1, r2));

    ReservationSummaryResponse s1 = mock(ReservationSummaryResponse.class);
    ReservationSummaryResponse s2 = mock(ReservationSummaryResponse.class);
    when(mapper.toSummary(r1)).thenReturn(s1);
    when(mapper.toSummary(r2)).thenReturn(s2);

    List<ReservationSummaryResponse> out = service.listMyReservations(userId);

    assertEquals(List.of(s1, s2), out);
    verify(mapper, times(1)).toSummary(r1);
    verify(mapper, times(1)).toSummary(r2);
  }

  // ========================= getMyReservation =========================

  @Test
  @DisplayName("getMyReservation → branch: reservation exists")
  void getMyReservation_exists() {
    // Testing method: getMyReservation; branch: found
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(r)).thenReturn(detail);

    ReservationDetailResponse out = service.getMyReservation(userId, id);

    assertSame(detail, out);
    verify(mapper, times(1)).toDetail(r);
  }

  @Test
  @DisplayName("getMyReservation → branch: reservation not found (throws NotFoundException)")
  void getMyReservation_notFound() {
    // Testing method: getMyReservation; branch: not found -> throws
    Long userId = 9L, id = 404L;
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.getMyReservation(userId, id));
    verify(mapper, never()).toDetail(any());
  }

  // ========================= createReservation =========================

  @Test
  @DisplayName("createReservation → branch: numGuests is null (throws BadRequestException)")
  void createReservation_numGuestsNull_throws() {
    // Testing method: createReservation; branch: validation numGuests == null
    Long userId = 9L;
    CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(null);

    // guards are called first
    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    assertThrows(BadRequestException.class, () -> service.createReservation(userId, req));

    verifyNoInteractions(nightsService, inventoryService, reservationsRepository, mapper);
  }

  @Test
  @DisplayName("createReservation → branch: numGuests <= 0 (throws BadRequestException)")
  void createReservation_numGuestsNonPositive_throws() {
    // Testing method: createReservation; branch: validation numGuests <= 0
    Long userId = 9L;
    CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(0);

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    assertThrows(BadRequestException.class, () -> service.createReservation(userId, req));
    verifyNoInteractions(nightsService, inventoryService, reservationsRepository, mapper);
  }

  @Test
  @DisplayName("createReservation → branch: priceTotal < 0 (throws BadRequestException)")
  void createReservation_negativePrice_throws() {
    // Testing method: createReservation; branch: validation priceTotal < 0
    Long userId = 9L;
    CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(2);
    when(req.getPriceTotal()).thenReturn(new BigDecimal("-0.01"));

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    assertThrows(BadRequestException.class, () -> service.createReservation(userId, req));
    verifyNoInteractions(nightsService, inventoryService, reservationsRepository, mapper);
  }

  @Test
  @DisplayName("createReservation → branch: happy path (guards OK, currency defaulted, nights computed, inventory reserved, save & map)")
  void createReservation_happyPath_currencyDefault() {
    // Testing method: createReservation; branch: valid flow
    Long userId = 9L;
    LocalDate in = LocalDate.of(2025, 10, 20);
    LocalDate out = LocalDate.of(2025, 10, 22);

    CreateReservationRequest req = mock(CreateReservationRequest.class);
    when(req.getHotelId()).thenReturn(1L);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getNumGuests()).thenReturn(2);
    when(req.getCurrency()).thenReturn(null); // expect default "USD"
    when(req.getPriceTotal()).thenReturn(null); // keep null, allowed
    when(req.getCheckInDate()).thenReturn(in);
    when(req.getCheckOutDate()).thenReturn(out);

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 11L);

    // nightsService should set dates & nights on the same instance and return it
    Mockito.lenient().when(nightsService.recalcNightsOrThrow(any(Reservations.class), eq(in), eq(out)))
        .thenAnswer(inv -> {
          Reservations r = inv.getArgument(0);
          r.setCheckInDate(in);
          r.setCheckOutDate(out);
          r.setNights((int) (out.toEpochDay() - in.toEpochDay()));
          return r;
        });

    // repository.save returns the same instance
    ArgumentCaptor<Reservations> saveCap = ArgumentCaptor.forClass(Reservations.class);
    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(any(Reservations.class))).thenReturn(detail);

    ReservationDetailResponse outResp = service.createReservation(userId, req);

    assertSame(detail, outResp);

    // verify inventory reserved with computed dates
    verify(inventoryService, times(1))
        .reserveRangeOrThrow(1L, 11L, in, out);

    // verify save values (currency default applied)
    verify(reservationsRepository).save(saveCap.capture());
    Reservations saved = saveCap.getValue();
    assertEquals(userId, saved.getUserId());
    assertEquals(1L, saved.getHotelId());
    assertEquals(11L, saved.getRoomTypeId());
    assertEquals(2, saved.getNumGuests());
    assertEquals("USD", saved.getCurrency(), "Currency should default to USD when req.getCurrency() == null");
    assertNull(saved.getPriceTotal(), "price_total remains null as passed");
    assertEquals(in, saved.getCheckInDate());
    assertEquals(out, saved.getCheckOutDate());
    assertEquals(2, saved.getNights());
  }

  // ========================= patchMyReservation =========================

  @Test
  @DisplayName("patchMyReservation → branch: reservation not found (throws NotFoundException)")
  void patchMyReservation_notFound() {
    // Testing method: patchMyReservation; branch: not found -> throws
    Long userId = 9L, id = 404L;
    PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.patchMyReservation(userId, id, req));
    // Repository should be queried exactly once, and no writes should happen
    verify(reservationsRepository, times(1)).findByIdAndUserId(id, userId);
    verify(reservationsRepository, never()).save(any());
    verifyNoMoreInteractions(reservationsRepository);
  }

  @Test
  @DisplayName("patchMyReservation → branch: dates changed (release old, recalc nights, reserve new, save & map)")
  void patchMyReservation_datesChanged_flow() {
    // Testing method: patchMyReservation; branch: re-date path
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation(); // old in=2025-10-20, out=2025-10-22
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    LocalDate newIn = LocalDate.of(2025, 10, 23);
    LocalDate newOut = LocalDate.of(2025, 10, 26);

    PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(newIn);
    when(req.getCheckOutDate()).thenReturn(newOut);
    when(req.getNumGuests()).thenReturn(null);

    // release old range first
    doNothing().when(inventoryService).releaseRange(1L, 11L, r.getCheckInDate(), r.getCheckOutDate());

    // recalc nights should set to new values on same instance
    when(nightsService.recalcNightsOrThrow(eq(r), eq(newIn), eq(newOut))).thenAnswer(inv -> {
      Reservations rr = inv.getArgument(0);
      rr.setCheckInDate(newIn);
      rr.setCheckOutDate(newOut);
      rr.setNights((int) (newOut.toEpochDay() - newIn.toEpochDay()));
      return rr;
    });

    // reserve new range
    doNothing().when(inventoryService).reserveRangeOrThrow(1L, 11L, newIn, newOut);

    when(reservationsRepository.save(r)).thenReturn(r);
    ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(r)).thenReturn(detail);

    ReservationDetailResponse out = service.patchMyReservation(userId, id, req);

    assertSame(detail, out);
    verify(inventoryService, times(1)).releaseRange(1L, 11L, LocalDate.of(2025,10,20), LocalDate.of(2025,10,22));
    verify(nightsService, times(1)).recalcNightsOrThrow(r, newIn, newOut);
    verify(inventoryService, times(1)).reserveRangeOrThrow(1L, 11L, newIn, newOut);
    verify(reservationsRepository, times(1)).save(r);
    assertEquals(3, r.getNights());
  }

  @Test
  @DisplayName("patchMyReservation → branch: only numGuests changed (valid positive) → save & map; no inventory/nights")
  void patchMyReservation_onlyNumGuests_valid() {
    // Testing method: patchMyReservation; branch: only update numGuests valid
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(null);
    when(req.getCheckOutDate()).thenReturn(null);
    when(req.getNumGuests()).thenReturn(4);

    when(reservationsRepository.save(r)).thenReturn(r);
    ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(r)).thenReturn(detail);

    ReservationDetailResponse out = service.patchMyReservation(userId, id, req);

    assertSame(detail, out);
    assertEquals(4, r.getNumGuests());
    verifyNoInteractions(nightsService, inventoryService);
    verify(reservationsRepository, times(1)).save(r);
  }

  @Test
  @DisplayName("patchMyReservation → branch: only numGuests changed (<=0 invalid) → throws BadRequestException")
  void patchMyReservation_onlyNumGuests_invalid() {
    // Testing method: patchMyReservation; branch: only update numGuests invalid (<=0)
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(null);
    when(req.getCheckOutDate()).thenReturn(null);
    when(req.getNumGuests()).thenReturn(0);

    assertThrows(BadRequestException.class, () -> service.patchMyReservation(userId, id, req));
    verify(reservationsRepository, never()).save(any());
    verifyNoInteractions(nightsService, inventoryService, mapper);
  }

  @Test
  @DisplayName("patchMyReservation → branch: no changes (dates null & numGuests null) → save & map only")
  void patchMyReservation_noChanges() {
    // Testing method: patchMyReservation; branch: no-op fields then save
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(null);
    when(req.getCheckOutDate()).thenReturn(null);
    when(req.getNumGuests()).thenReturn(null);

    when(reservationsRepository.save(r)).thenReturn(r);
    ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(r)).thenReturn(detail);

    ReservationDetailResponse out = service.patchMyReservation(userId, id, req);

    assertSame(detail, out);
    verifyNoInteractions(nightsService, inventoryService);
    verify(reservationsRepository, times(1)).save(r);
  }

  // ========================= cancelMyReservation =========================

  @Test
  @DisplayName("cancelMyReservation → branch: reservation not found (throws NotFoundException)")
  void cancelMyReservation_notFound() {
    // Testing method: cancelMyReservation; branch: not found -> throws
    Long userId = 9L, id = 404L;
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.cancelMyReservation(userId, id));
    verifyNoInteractions(orchestrator);
  }

  @Test
  @DisplayName("cancelMyReservation → branch: happy path (call orchestrator.cancel with reason 'user-cancel')")
  void cancelMyReservation_happyPath() {
    // Testing method: cancelMyReservation; branch: found -> orchestrate cancel
    Long userId = 9L, id = 100L;
    Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    doNothing().when(orchestrator).cancel(r, "user-cancel", userId);

    service.cancelMyReservation(userId, id);

    verify(orchestrator, times(1)).cancel(r, "user-cancel", userId);
  }
}
