package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationCoreServiceTest {

  @Mock
  ReservationsRepository reservationsRepository;

  @Mock
  ReservationsStatusHistoryRepository historyRepository;

  @Mock
  ReservationStatusMachine statusMachine;

  @InjectMocks
  ReservationCoreService coreService;

  @Test
  void recalcNightsOrThrow_validDates_setsFieldsAndSaves() {
    Reservations r = new Reservations();

    // Repository save returns the same instance for simplicity
    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    Reservations out = coreService.recalcNightsOrThrow(r, LocalDate.of(2025,1,1), LocalDate.of(2025,1,3));

    assertEquals(2, out.getNights());
    assertEquals(LocalDate.of(2025,1,1), out.getCheck_in_date());
    assertEquals(LocalDate.of(2025,1,3), out.getCheck_out_date());
    verify(reservationsRepository, times(1)).save(r);
  }

  @Test
  void recalcNightsOrThrow_nullDates_returnsWithoutSave() {
    Reservations r = new Reservations();
    Reservations out = coreService.recalcNightsOrThrow(r, null, LocalDate.of(2025,1,2));
    assertSame(r, out);
    verifyNoInteractions(reservationsRepository);
  }

  @Test
  void recalcNightsOrThrow_nonPositive_throws() {
    Reservations r = new Reservations();
    assertThrows(BadRequestException.class,
        () -> coreService.recalcNightsOrThrow(r, LocalDate.of(2025,1,2), LocalDate.of(2025,1,2)));
    assertThrows(BadRequestException.class,
        () -> coreService.recalcNightsOrThrow(r, LocalDate.of(2025,1,3), LocalDate.of(2025,1,2)));
    verifyNoInteractions(reservationsRepository);
  }

  @Test
  void changeStatus_allowed_savesReservationAndHistory() {
    Reservations r = new Reservations();
    r.setId(99L);
    r.setStatus(ReservationStatus.PENDING);

    when(statusMachine.canTransit(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)).thenReturn(true);
    when(reservationsRepository.save(any(Reservations.class))).thenAnswer(inv -> inv.getArgument(0));

    Reservations out = coreService.changeStatus(r, ReservationStatus.CONFIRMED, "reason", 123L);

    assertEquals(ReservationStatus.CONFIRMED, out.getStatus());
    // Verify history saved with correct fields
    ArgumentCaptor<ReservationsStatusHistory> hc = ArgumentCaptor.forClass(ReservationsStatusHistory.class);
    verify(historyRepository).save(hc.capture());
    ReservationsStatusHistory h = hc.getValue();
    assertEquals(99L, h.getReservation_id());
    assertEquals(ReservationStatus.PENDING, h.getFrom_status());
    assertEquals(ReservationStatus.CONFIRMED, h.getTo_status());
    assertEquals("reason", h.getReason());
    assertEquals(123L, h.getChanged_by_user_id());
    assertNotNull(h.getChanged_at());
  }

  @Test
  void changeStatus_illegalTransition_throws_noSaves() {
    Reservations r = new Reservations();
    r.setStatus(ReservationStatus.PENDING);

    when(statusMachine.canTransit(ReservationStatus.PENDING, ReservationStatus.CHECKED_OUT)).thenReturn(false);

    assertThrows(BadRequestException.class,
        () -> coreService.changeStatus(r, ReservationStatus.CHECKED_OUT, "boom", 1L));

    verifyNoInteractions(historyRepository);
    verify(reservationsRepository, never()).save(any());
  }

  @Test
  void cancel_alreadyCanceled_noop() {
    Reservations r = new Reservations();
    r.setStatus(ReservationStatus.CANCELED);

    // Spy to ensure changeStatus is not called
    ReservationCoreService spyCore = Mockito.spy(coreService);
    spyCore.cancel(r, "user-cancel", 7L);

    verify(spyCore, never()).changeStatus(any(), any(), anyString(), any());
    verifyNoInteractions(reservationsRepository, historyRepository);
    assertNull(r.getCanceled_at(), "canceled_at should not be set when already canceled");
  }

  @Test
  void cancel_checkedOut_throws() {
    Reservations r = new Reservations();
    r.setStatus(ReservationStatus.CHECKED_OUT);

    ReservationCoreService spyCore = Mockito.spy(coreService);

    assertThrows(BadRequestException.class, () -> spyCore.cancel(r, "user-cancel", 7L));

    verify(spyCore, never()).changeStatus(any(), any(), anyString(), any());
  }

  @Test
  void cancel_pending_callsChangeStatus_andSetsCanceledAt() {
    Reservations r = new Reservations();
    r.setId(1L);
    r.setStatus(ReservationStatus.PENDING);

    ReservationCoreService spyCore = Mockito.spy(coreService);
    // Stub the internal changeStatus call to avoid asserting its internals here
    doReturn(r).when(spyCore).changeStatus(eq(r), eq(ReservationStatus.CANCELED), eq("user-cancel"), eq(7L));

    spyCore.cancel(r, "user-cancel", 7L);

    verify(spyCore, times(1)).changeStatus(eq(r), eq(ReservationStatus.CANCELED), eq("user-cancel"), eq(7L));
    assertNotNull(r.getCanceled_at(), "canceled_at should be set on cancel");
  }
}
