package com.project.airhotel.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.hotel.domain.Hotels;
import com.project.airhotel.hotel.repository.HotelsRepository;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationDetailResponse;
import com.project.airhotel.reservation.dto.ReservationSummaryResponse;
import com.project.airhotel.reservation.mapper.ReservationMapper;
import com.project.airhotel.reservation.policy.UserReservationPolicy;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for UserReservationService. Each test states which method & which branch is being
 * exercised.
 */
@ExtendWith(MockitoExtension.class)
class UserReservationServiceTest {

  @Mock
  private ReservationsRepository reservationsRepository;

  @Mock
  private ReservationOrchestrator orchestrator;

  @Mock
  private ReservationMapper mapper;

  @Mock
  private HotelsRepository hotelsRepository;

  @Mock
  private RoomTypesRepository roomTypesRepository;

  @InjectMocks
  private UserReservationService service;

  private Reservations baseReservation() {
    final Reservations r = new Reservations();
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
  @DisplayName("listMyReservations → repository returns empty list → mapper never called")
  void listMyReservations_empty() {
    final Long userId = 9L;
    when(reservationsRepository.findByUserId(userId)).thenReturn(List.of());

    final List<ReservationSummaryResponse> out = service.listMyReservations(userId);

    assertNotNull(out);
    assertTrue(out.isEmpty());
    verify(mapper, never()).toSummary(any());
  }

  @Test
  @DisplayName("listMyReservations → non-empty list → mapping order preserved")
  void listMyReservations_nonEmpty() {
    final Long userId = 9L;
    final Reservations r1 = new Reservations();
    r1.setId(1L);
    r1.setHotelId(101L);
    r1.setRoomTypeId(201L);

    final Reservations r2 = new Reservations();
    r2.setId(2L);
    r2.setHotelId(102L);
    r2.setRoomTypeId(202L);
    when(reservationsRepository.findByUserId(userId)).thenReturn(List.of(r1, r2));

    final Hotels h1 = new Hotels();
    h1.setId(101L);
    h1.setName("H1");
    final Hotels h2 = new Hotels();
    h2.setId(102L);
    h2.setName("H2");
    when(hotelsRepository.findAllById(Mockito.any())).thenReturn(List.of(h1, h2));

    final RoomTypes rt1 = new RoomTypes();
    rt1.setId(201L);
    rt1.setName("RT1");
    final RoomTypes rt2 = new RoomTypes();
    rt2.setId(202L);
    rt2.setName("RT2");
    when(roomTypesRepository.findAllById(Mockito.any())).thenReturn(List.of(rt1, rt2));

    final ReservationSummaryResponse s1 = mock(ReservationSummaryResponse.class);
    final ReservationSummaryResponse s2 = mock(ReservationSummaryResponse.class);
    when(mapper.toSummary(r1)).thenReturn(s1);
    when(mapper.toSummary(r2)).thenReturn(s2);

    final List<ReservationSummaryResponse> out = service.listMyReservations(userId);

    assertEquals(List.of(s1, s2), out);
    verify(mapper).toSummary(r1);
    verify(mapper).toSummary(r2);
  }

  // ========================= getMyReservation =========================

  @Test
  @DisplayName("getMyReservation → found → mapped to detail")
  void getMyReservation_exists() {
    final Long userId = 9L;
    final Long id = 100L;
    final Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(r));

    final ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(r)).thenReturn(detail);

    final ReservationDetailResponse out = service.getMyReservation(userId, id);

    assertSame(detail, out);
    verify(mapper).toDetail(r);
  }

  @Test
  @DisplayName("getMyReservation → not found → throws NotFoundException")
  void getMyReservation_notFound() {
    when(reservationsRepository.findByIdAndUserId(404L, 9L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.getMyReservation(9L, 404L));
    verify(mapper, never()).toDetail(any());
  }

  // ========================= createReservation =========================

  @Test
  @DisplayName("createReservation → delegates to orchestrator.createReservation → mapped to detail")
  void createReservation_delegatesToOrchestrator() {
    final Long userId = 9L;
    final CreateReservationRequest req = mock(CreateReservationRequest.class);

    final Reservations saved = baseReservation();
    when(orchestrator.createReservation(userId, req)).thenReturn(saved);

    final ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(saved)).thenReturn(detail);

    final ReservationDetailResponse out = service.createReservation(userId, req);

    assertSame(detail, out);
    verify(orchestrator).createReservation(userId, req);
    verify(mapper).toDetail(saved);
  }

  // ========================= patchMyReservation =========================

  @Test
  @DisplayName("patchMyReservation → not found → throws NotFoundException")
  void patchMyReservation_notFound() {
    when(reservationsRepository.findByIdAndUserId(404L, 9L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class,
        () -> service.patchMyReservation(9L, 404L, mock(PatchReservationRequest.class)));
    verify(orchestrator, never()).modifyReservation(anyLong(), any(), any(), any());
  }

  @Test
  @DisplayName("patchMyReservation → happy path: dates change only → orchestrator called with"
      + " UserReservationPolicy, then mapped")
  void patchMyReservation_datesChange() {
    final Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(100L, 9L)).thenReturn(Optional.of(r));

    final PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(LocalDate.of(2025, 10, 23));
    when(req.getCheckOutDate()).thenReturn(LocalDate.of(2025, 10, 25));
    when(req.getNumGuests()).thenReturn(null);

    final Reservations updated = baseReservation();
    updated.setCheckInDate(LocalDate.of(2025, 10, 23));
    updated.setCheckOutDate(LocalDate.of(2025, 10, 25));

    when(orchestrator.modifyReservation(eq(r.getHotelId()), eq(r), any(),
        any(UserReservationPolicy.class)))
        .thenReturn(updated);

    final ReservationDetailResponse detail = mock(ReservationDetailResponse.class);
    when(mapper.toDetail(updated)).thenReturn(detail);

    final ReservationDetailResponse out = service.patchMyReservation(9L, 100L, req);

    assertSame(detail, out);
    verify(orchestrator).modifyReservation(eq(1L), same(r), argThat(ch ->
        ch.newCheckIn() != null && ch.newCheckOut() != null && ch.newNumGuests() == null
    ), any(UserReservationPolicy.class));
  }

  @Test
  @DisplayName("patchMyReservation → invalid numGuests (orchestrator throws) → propagated")
  void patchMyReservation_invalidGuests_propagates() {
    final Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(100L, 9L)).thenReturn(Optional.of(r));

    final PatchReservationRequest req = mock(PatchReservationRequest.class);
    when(req.getCheckInDate()).thenReturn(null);
    when(req.getCheckOutDate()).thenReturn(null);
    when(req.getNumGuests()).thenReturn(0);

    doThrow(new BadRequestException("numGuests must be positive."))
        .when(orchestrator)
        .modifyReservation(eq(1L), same(r), any(), any(UserReservationPolicy.class));

    assertThrows(BadRequestException.class,
        () -> service.patchMyReservation(9L, 100L, req));
  }

  // ========================= cancelMyReservation =========================

  @Test
  @DisplayName("cancelMyReservation → not found → throws NotFoundException")
  void cancelMyReservation_notFound() {
    when(reservationsRepository.findByIdAndUserId(404L, 9L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.cancelMyReservation(9L, 404L));
    verify(orchestrator, never()).cancel(any(), any(), any());
  }

  @Test
  @DisplayName("cancelMyReservation → found → delegates to orchestrator.cancel with "
      + "reason 'user-cancel'")
  void cancelMyReservation_delegates() {
    final Reservations r = baseReservation();
    when(reservationsRepository.findByIdAndUserId(100L, 9L)).thenReturn(Optional.of(r));

    doNothing().when(orchestrator).cancel(r, "user-cancel", 9L);

    service.cancelMyReservation(9L, 100L);

    verify(orchestrator).cancel(r, "user-cancel", 9L);
  }
}
