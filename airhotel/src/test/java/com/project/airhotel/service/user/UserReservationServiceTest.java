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
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationInventoryService;
import com.project.airhotel.service.core.ReservationNightsService;
import com.project.airhotel.service.core.ReservationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReservationServiceTest {

  @Mock ReservationsRepository reservationsRepository;
  @Mock ReservationNightsService nightsService;
  @Mock ReservationInventoryService inventoryService;
  @Mock ReservationOrchestrator orchestrator;
  @Mock EntityGuards entityGuards;

  ReservationMapper mapper = new ReservationMapper();

  @InjectMocks UserReservationService service;

  @BeforeEach
  void init() {
    service = new UserReservationService(reservationsRepository, nightsService, inventoryService, orchestrator, mapper, entityGuards);
  }

  private Reservations makeReservationEntity(Long id) {
    Reservations r = new Reservations();
    r.setId(id);
    r.setStatus(ReservationStatus.PENDING);
    r.setUpgrade_status(UpgradeStatus.NOT_ELIGIBLE);
    r.setCheck_in_date(LocalDate.of(2025, 11, 1));
    r.setCheck_out_date(LocalDate.of(2025, 11, 3));
    r.setNights(2);
    r.setNum_guests(2);
    r.setCurrency("USD");
    r.setPrice_total(new BigDecimal("123.45"));
    r.setCreated_at(LocalDateTime.of(2025, 10, 1, 12, 0));
    return r;
  }

  @Test
  void listMyReservations_mapsEntitiesToSummaries() {
    Reservations r = makeReservationEntity(10L);
    when(reservationsRepository.findByUserId(1L)).thenReturn(List.of(r));

    List<ReservationSummaryResponse> list = service.listMyReservations(1L);

    assertEquals(1, list.size());
    ReservationSummaryResponse s = list.get(0);
    assertEquals(10L, s.getId());
    assertEquals(ReservationStatus.PENDING, s.getStatus());
    assertEquals(2, s.getNights());
    assertEquals(2, s.getNumGuests());
    assertEquals(new BigDecimal("123.45"), s.getPriceTotal());
    assertNotNull(s.getCreatedAt(), "createdAt should be mapped when present");
  }

  @Test
  void getMyReservation_found_returnsDetail() {
    Reservations r = makeReservationEntity(22L);
    when(reservationsRepository.findByIdAndUserId(22L, 5L)).thenReturn(Optional.of(r));

    ReservationDetailResponse d = service.getMyReservation(5L, 22L);

    assertEquals(22L, d.getId());
    assertEquals(ReservationStatus.PENDING, d.getStatus());
    assertEquals("USD", d.getCurrency());
    assertEquals(new BigDecimal("123.45"), d.getPriceTotal());
    assertEquals(2, d.getNights());
  }

  @Test
  void getMyReservation_notFound_throws() {
    when(reservationsRepository.findByIdAndUserId(9L, 3L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.getMyReservation(3L, 9L));
  }

  @Test
  void createReservation_callsNightsAndInventory_andSaves() {
    CreateReservationRequest req = new CreateReservationRequest();
    req.setHotelId(1L);
    req.setRoomTypeId(2L);
    req.setCheckInDate(LocalDate.of(2025, 12, 1));
    req.setCheckOutDate(LocalDate.of(2025, 12, 4));
    req.setNumGuests(3);
    req.setCurrency(null);
    req.setPriceTotal(new BigDecimal("999.99"));

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 2L);

    Mockito.doAnswer(inv -> {
      Reservations r = inv.getArgument(0);
      r.setCheck_in_date(req.getCheckInDate());
      r.setCheck_out_date(req.getCheckOutDate());
      r.setNights(3);
      return r;
    }).when(nightsService).recalcNightsOrThrow(any(Reservations.class), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));

    doNothing().when(inventoryService).reserveRangeOrThrow(eq(1L), eq(2L), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));

    Reservations saved = new Reservations();
    saved.setId(777L);
    saved.setStatus(ReservationStatus.PENDING);
    saved.setUpgrade_status(UpgradeStatus.NOT_ELIGIBLE);
    saved.setCheck_in_date(req.getCheckInDate());
    saved.setCheck_out_date(req.getCheckOutDate());
    saved.setNights(3);
    saved.setNum_guests(3);
    saved.setCurrency("USD"); // fallback
    saved.setPrice_total(new BigDecimal("999.99"));

    when(reservationsRepository.save(any(Reservations.class))).thenReturn(saved);

    ReservationDetailResponse out = service.createReservation(9L, req);

    verify(entityGuards).ensureHotelExists(1L);
    verify(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 2L);
    verify(nightsService).recalcNightsOrThrow(any(Reservations.class), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));
    verify(inventoryService).reserveRangeOrThrow(eq(1L), eq(2L), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));
    verify(reservationsRepository, atLeastOnce()).save(any());

    assertEquals(777L, out.getId());
    assertEquals(3, out.getNights());
    assertEquals(ReservationStatus.PENDING, out.getStatus());
    assertEquals("USD", out.getCurrency());
    assertEquals(0, out.getPriceTotal().compareTo(new BigDecimal("999.99")));
  }

  @Test
  void patchMyReservation_updateDatesAndGuests_callsNightsAndInventory_andSaves() {
    Reservations existing = makeReservationEntity(55L);
    existing.setCheck_in_date(LocalDate.of(2025, 11, 1));
    existing.setCheck_out_date(LocalDate.of(2025, 11, 3));

    when(reservationsRepository.findByIdAndUserId(55L, 2L)).thenReturn(Optional.of(existing));
    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    PatchReservationRequest req = new PatchReservationRequest();
    req.setCheckInDate(LocalDate.of(2025, 11, 2));
    req.setCheckOutDate(LocalDate.of(2025, 11, 5));
    req.setNumGuests(4);

    doAnswer(inv -> {
      Reservations r = inv.getArgument(0);
      r.setCheck_in_date(req.getCheckInDate());
      r.setCheck_out_date(req.getCheckOutDate());
      r.setNights(3);
      return r;
    }).when(nightsService).recalcNightsOrThrow(eq(existing), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));

    doNothing().when(inventoryService).releaseRange(eq(existing.getHotel_id()), eq(existing.getRoom_type_id()),
        eq(LocalDate.of(2025, 11, 1)), eq(LocalDate.of(2025, 11, 3)));
    doNothing().when(inventoryService).reserveRangeOrThrow(eq(existing.getHotel_id()), eq(existing.getRoom_type_id()),
        eq(LocalDate.of(2025, 11, 2)), eq(LocalDate.of(2025, 11, 5)));

    ReservationDetailResponse out = service.patchMyReservation(2L, 55L, req);

    verify(nightsService).recalcNightsOrThrow(eq(existing), eq(req.getCheckInDate()), eq(req.getCheckOutDate()));
    verify(inventoryService).releaseRange(eq(existing.getHotel_id()), eq(existing.getRoom_type_id()),
        eq(LocalDate.of(2025, 11, 1)), eq(LocalDate.of(2025, 11, 3)));
    verify(inventoryService).reserveRangeOrThrow(eq(existing.getHotel_id()), eq(existing.getRoom_type_id()),
        eq(LocalDate.of(2025, 11, 2)), eq(LocalDate.of(2025, 11, 5)));
    verify(reservationsRepository).save(existing);

    assertEquals(4, out.getNumGuests());
    assertEquals(3, out.getNights());
    assertEquals(LocalDate.of(2025, 11, 2), out.getCheckInDate());
    assertEquals(LocalDate.of(2025, 11, 5), out.getCheckOutDate());
  }

  @Test
  void patchMyReservation_onlyNumGuests_doesNotInvokeNights() {
    Reservations existing = makeReservationEntity(66L);
    when(reservationsRepository.findByIdAndUserId(66L, 8L)).thenReturn(Optional.of(existing));
    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    PatchReservationRequest req = new PatchReservationRequest();
    req.setNumGuests(5);

    ReservationDetailResponse out = service.patchMyReservation(8L, 66L, req);

    verify(nightsService, never()).recalcNightsOrThrow(any(), any(), any());
    verify(inventoryService, never()).releaseRange(any(), any(), any(), any());
    verify(inventoryService, never()).reserveRangeOrThrow(any(), any(), any(), any());
    verify(reservationsRepository).save(existing);

    assertEquals(5, out.getNumGuests());
    assertEquals(existing.getNights(), out.getNights());
  }

  @Test
  void cancelMyReservation_found_callsOrchestrator() {
    Reservations existing = makeReservationEntity(77L);
    when(reservationsRepository.findByIdAndUserId(77L, 9L)).thenReturn(Optional.of(existing));

    service.cancelMyReservation(9L, 77L);

    verify(orchestrator).cancel(existing, "user-cancel", 9L);
  }

  @Test
  void cancelMyReservation_notFound_throws() {
    when(reservationsRepository.findByIdAndUserId(100L, 9L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.cancelMyReservation(9L, 100L));
  }

  @Test
  void createReservation_invalidGuests_throws() {
    CreateReservationRequest req = new CreateReservationRequest();
    req.setHotelId(1L);
    req.setRoomTypeId(2L);
    req.setCheckInDate(LocalDate.of(2025, 12, 1));
    req.setCheckOutDate(LocalDate.of(2025, 12, 4));
    req.setNumGuests(0);
    req.setPriceTotal(new BigDecimal("1"));

    doNothing().when(entityGuards).ensureHotelExists(1L);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(1L, 2L);

    assertThrows(BadRequestException.class, () -> service.createReservation(9L, req));
    verify(nightsService, never()).recalcNightsOrThrow(any(), any(), any());
    verify(inventoryService, never()).reserveRangeOrThrow(any(), any(), any(), any());
    verify(reservationsRepository, never()).save(any());
  }
}
