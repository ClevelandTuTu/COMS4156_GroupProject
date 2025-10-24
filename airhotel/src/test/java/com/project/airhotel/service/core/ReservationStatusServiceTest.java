package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.project.airhotel.model.enums.ReservationStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationStatusService.
 * Each test states which method & branch is being exercised.
 */
@ExtendWith(MockitoExtension.class)
class ReservationStatusServiceTest {

  @Mock
  ReservationsRepository reservationsRepository;

  @Mock
  ReservationsStatusHistoryRepository historyRepository;

  @Mock
  ReservationStatusMachine statusMachine;

  @InjectMocks
  ReservationStatusService service;

  private Reservations reservationWithStatus(final ReservationStatus status) {
    final Reservations r = new Reservations();
    r.setId(42L);
    r.setStatus(status);
    return r;
  }

  @Test
  @DisplayName("changeStatus → branch: illegal transition (statusMachine.canTransit=false) -> throws and no persistence")
  void changeStatus_illegalTransition_throws_noSideEffects() {
    // Testing method: changeStatus; branch: illegal transition
    final Reservations r = reservationWithStatus(PENDING);
    when(statusMachine.canTransit(PENDING, CHECKED_IN)).thenReturn(false);

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.changeStatus(r, CHECKED_IN, "reason", 100L)
    );
    assertTrue(ex.getMessage().contains("Illegal status transition"));

    // Ensure nothing persisted when transition is illegal
    verifyNoInteractions(reservationsRepository, historyRepository);
    // Ensure original status remains unchanged in memory
    assertEquals(PENDING, r.getStatus());
  }

  @Test
  @DisplayName("changeStatus → branch: legal transition (PENDING -> CONFIRMED) with reason & userId")
  void changeStatus_legalTransition_persistsReservationAndHistory() {
    // Testing method: changeStatus; branch: legal transition + non-null reason/userId
    final Reservations r = reservationWithStatus(PENDING);
    when(statusMachine.canTransit(PENDING, CONFIRMED)).thenReturn(true);

    // verify the object passed to repository has status set to target before save
    Mockito.lenient().when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> {
      final Reservations arg = inv.getArgument(0);
      // status should already be set to CONFIRMED when calling save
      assertEquals(CONFIRMED, arg.getStatus(), "Reservation must be updated to target status before saving");
      // simulate DB saved object (could be same instance or a copy)
      return arg;
    });

    // capture history entity to assert its fields
    final ArgumentCaptor<ReservationsStatusHistory> hCap = ArgumentCaptor.forClass(ReservationsStatusHistory.class);

    final LocalDateTime before = LocalDateTime.now();
    final Reservations saved = service.changeStatus(r, CONFIRMED, "manual-approve", 999L);
    final LocalDateTime after = LocalDateTime.now();

    // asserts on returned entity
    assertNotNull(saved);
    assertEquals(CONFIRMED, saved.getStatus());
    assertEquals(42L, saved.getId()); // id carried through from original in this stub setup

    // verify persistence interactions
    verify(reservationsRepository, times(1)).save(r);
    verify(historyRepository, times(1)).save(hCap.capture());
    verify(statusMachine, times(1)).canTransit(PENDING, CONFIRMED);

    // assert history fields
    final ReservationsStatusHistory h = hCap.getValue();
    assertEquals(42L, h.getReservationId());
    assertEquals(PENDING, h.getFromStatus());
    assertEquals(CONFIRMED, h.getToStatus());
    assertEquals("manual-approve", h.getReason());
    assertEquals(999L, h.getChangedByUserId());
    assertNotNull(h.getChangedAt());
    assertFalse(h.getChangedAt().isBefore(before));
    assertFalse(h.getChangedAt().isAfter(after));
  }

  @Test
  @DisplayName("changeStatus → branch: legal transition (CONFIRMED -> CHECKED_IN) with null reason & null userId")
  void changeStatus_legalTransition_nullReasonAndUser() {
    // Testing method: changeStatus; branch: legal transition + null reason/userId
    final Reservations r = reservationWithStatus(CONFIRMED);
    when(statusMachine.canTransit(CONFIRMED, CHECKED_IN)).thenReturn(true);

    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    final ArgumentCaptor<ReservationsStatusHistory> hCap = ArgumentCaptor.forClass(ReservationsStatusHistory.class);

    service.changeStatus(r, CHECKED_IN, null, null);

    // reservation persisted with updated status
    verify(reservationsRepository, times(1)).save(r);
    assertEquals(CHECKED_IN, r.getStatus());

    // history persisted with null fields for reason/user
    verify(historyRepository, times(1)).save(hCap.capture());
    final ReservationsStatusHistory h = hCap.getValue();
    assertEquals(CONFIRMED, h.getFromStatus());
    assertEquals(CHECKED_IN, h.getToStatus());
    assertNull(h.getReason());
    assertNull(h.getChangedByUserId());
    assertNotNull(h.getChangedAt());
  }

  @Test
  @DisplayName("changeStatus → repository save throws; no history is written and exception propagates")
  void changeStatus_repositorySaveThrows_noHistory() {
    final Reservations r = reservationWithStatus(PENDING);
    when(statusMachine.canTransit(PENDING, CONFIRMED)).thenReturn(true);
    when(reservationsRepository.save(any(Reservations.class)))
        .thenThrow(new RuntimeException("db-error"));

    assertThrows(RuntimeException.class,
        () -> service.changeStatus(r, CONFIRMED, "x", 1L));

    verify(historyRepository, never()).save(any(ReservationsStatusHistory.class));
    assertEquals(CONFIRMED, r.getStatus());
  }

  @Test
  @DisplayName("changeStatus → history uses saved entity id (not original)")
  void changeStatus_historyUsesSavedId() {
    final Reservations r = reservationWithStatus(PENDING);
    r.setId(1L);
    when(statusMachine.canTransit(PENDING, CONFIRMED)).thenReturn(true);

    final Reservations persisted = reservationWithStatus(CONFIRMED);
    persisted.setId(777L);
    when(reservationsRepository.save(any(Reservations.class))).thenReturn(persisted);

    final ArgumentCaptor<ReservationsStatusHistory> cap = ArgumentCaptor.forClass(ReservationsStatusHistory.class);
    final Reservations out = service.changeStatus(r, CONFIRMED, null, null);

    verify(historyRepository).save(cap.capture());
    assertEquals(777L, cap.getValue().getReservationId());
    assertEquals(CONFIRMED, out.getStatus());
  }

}
