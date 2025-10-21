package com.project.airhotel;

import com.project.airhotel.dto.reservations.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservations.ReservationUpdateRequest;
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
import com.project.airhotel.service.manager.ManagerReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Ziyang Su
 * @version 1.0.1
 */
@ExtendWith(MockitoExtension.class)
public class ManagerReservationServiceTest {

  @Mock ReservationsRepository reservationsRepository;
  @Mock EntityGuards guards;

  @Mock ReservationNightsService nightsService;
  @Mock ReservationInventoryService inventoryService;
  @Mock ReservationStatusService statusService;
  @Mock ReservationOrchestrator orchestrator;

  @InjectMocks ManagerReservationService service;

  private Reservations sampleRes;

  @BeforeEach
  void setUp() {
    sampleRes = new Reservations();
    sampleRes.setId(100L);
    sampleRes.setHotel_id(1L);
    sampleRes.setRoom_type_id(10L);
    sampleRes.setRoom_id(88L);
    sampleRes.setStatus(ReservationStatus.PENDING);
    sampleRes.setCheck_in_date(LocalDate.of(2025, 10, 20));
    sampleRes.setCheck_out_date(LocalDate.of(2025, 10, 22));
    sampleRes.setNights(2);
    sampleRes.setNum_guests(2);
    sampleRes.setCurrency("USD");
    sampleRes.setPrice_total(new BigDecimal("200.00"));
    sampleRes.setUpgrade_status(UpgradeStatus.NOT_ELIGIBLE);
  }

  // ---------- listReservations ----------

  @Test
  void listReservations_byStatus() {
    when(reservationsRepository.findByHotelIdAndStatus(1L, ReservationStatus.CONFIRMED))
        .thenReturn(List.of(sampleRes));

    var list = service.listReservations(1L, ReservationStatus.CONFIRMED, null, null);

    verify(guards).ensureHotelExists(1L);
    verify(reservationsRepository).findByHotelIdAndStatus(1L, ReservationStatus.CONFIRMED);
    assertThat(list).hasSize(1);
  }

  @Test
  void listReservations_byDateRange() {
    LocalDate start = LocalDate.of(2025, 10, 1);
    LocalDate end = LocalDate.of(2025, 10, 31);

    when(reservationsRepository.findByHotelIdAndStayRange(1L, start, end))
        .thenReturn(List.of(sampleRes));

    var list = service.listReservations(1L, null, start, end);

    verify(guards).ensureHotelExists(1L);
    verify(reservationsRepository).findByHotelIdAndStayRange(1L, start, end);
    assertThat(list).hasSize(1);
  }

  @Test
  void listReservations_all() {
    when(reservationsRepository.findByHotelId(1L)).thenReturn(List.of(sampleRes));

    var list = service.listReservations(1L, null, null, null);

    verify(guards).ensureHotelExists(1L);
    verify(reservationsRepository).findByHotelId(1L);
    assertThat(list).hasSize(1);
  }

  // ---------- getReservation ----------

  @Test
  void getReservation_ok() {
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    var r = service.getReservation(1L, 100L);

    verify(guards).getReservationInHotelOrThrow(1L, 100L);
    assertThat(r.getId()).isEqualTo(100L);
  }

  // ---------- patchReservation ----------

  @Test
  void patchReservation_updateDates_computesNights_andReservesInventory() {
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);
    when(reservationsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    doAnswer(inv -> {
      Reservations r = inv.getArgument(0);
      LocalDate cin = inv.getArgument(1);
      LocalDate cout = inv.getArgument(2);
      r.setCheck_in_date(cin);
      r.setCheck_out_date(cout);
      r.setNights((int)(cout.toEpochDay() - cin.toEpochDay()));
      return r;
    }).when(nightsService).recalcNightsOrThrow(any(Reservations.class), any(LocalDate.class), any(LocalDate.class));

    var req = new ReservationUpdateRequest();
    req.setCheckInDate(LocalDate.of(2025, 10, 20));
    req.setCheckOutDate(LocalDate.of(2025, 10, 23));
    req.setNumGuests(3);
    req.setCurrency("USD");
    req.setPriceTotal(new BigDecimal("300.00"));
    req.setNotes("late arrival");

    var updated = service.patchReservation(1L, 100L, req);

    assertThat(updated.getNights()).isEqualTo(3);
    assertThat(updated.getNum_guests()).isEqualTo(3);
    assertThat(updated.getPrice_total()).isEqualByComparingTo("300.00");
    assertThat(updated.getNotes()).isEqualTo("late arrival");

    verify(inventoryService).releaseRange(eq(1L), eq(10L), eq(LocalDate.of(2025, 10, 20)), eq(LocalDate.of(2025, 10, 22)));
    verify(nightsService).recalcNightsOrThrow(eq(sampleRes), eq(LocalDate.of(2025, 10, 20)), eq(LocalDate.of(2025, 10, 23)));
    verify(inventoryService).reserveRangeOrThrow(eq(1L), eq(10L), eq(LocalDate.of(2025, 10, 20)), eq(LocalDate.of(2025, 10, 23)));
    verify(reservationsRepository, atLeastOnce()).save(any());
  }

  @Test
  void patchReservation_invalidDates_throwBadRequest() {
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);
    doThrow(new BadRequestException("Check out date must be later than check in date."))
        .when(nightsService).recalcNightsOrThrow(any(Reservations.class), any(LocalDate.class), any(LocalDate.class));

    var req = new ReservationUpdateRequest();
    req.setCheckInDate(LocalDate.of(2025, 10, 22));
    req.setCheckOutDate(LocalDate.of(2025, 10, 22));

    assertThatThrownBy(() -> service.patchReservation(1L, 100L, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Check out date must be later");

    verify(nightsService).recalcNightsOrThrow(eq(sampleRes), eq(LocalDate.of(2025, 10, 22)), eq(LocalDate.of(2025, 10, 22)));
    verify(reservationsRepository, never()).save(any());
  }

  @Test
  void patchReservation_changeStatus_writesHistory_via_statusService() {
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);
    when(reservationsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(statusService.changeStatus(any(Reservations.class), eq(ReservationStatus.CONFIRMED), any(), any()))
        .thenAnswer(inv -> {
          Reservations r = inv.getArgument(0);
          r.setStatus(ReservationStatus.CONFIRMED);
          return r;
        });

    var req = new ReservationUpdateRequest();
    req.setStatus(ReservationStatus.CONFIRMED);

    var updated = service.patchReservation(1L, 100L, req);

    assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    verify(statusService).changeStatus(eq(sampleRes), eq(ReservationStatus.CONFIRMED), isNull(), isNull());
  }

  // ---------- applyUpgrade ----------

  @Test
  void applyUpgrade_ok() {
    sampleRes.setUpgrade_status(UpgradeStatus.ELIGIBLE);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);
    doNothing().when(guards).ensureRoomTypeInHotelOrThrow(1L, 99L);
    when(reservationsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var req = new ApplyUpgradeRequest();
    req.setNewRoomTypeId(99L);

    var r = service.applyUpgrade(1L, 100L, req);

    assertThat(r.getRoom_type_id()).isEqualTo(99L);
    assertThat(r.getUpgrade_status()).isEqualTo(UpgradeStatus.APPLIED);

    verify(inventoryService).releaseRange(eq(1L), eq(10L), eq(sampleRes.getCheck_in_date()), eq(sampleRes.getCheck_out_date()));
    verify(inventoryService).reserveRangeOrThrow(eq(1L), eq(99L), eq(sampleRes.getCheck_in_date()), eq(sampleRes.getCheck_out_date()));
    verify(reservationsRepository).save(any(Reservations.class));
  }

  @Test
  void applyUpgrade_alreadyApplied_noThrow_andSaves() {
    sampleRes.setUpgrade_status(UpgradeStatus.APPLIED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);
    doNothing().when(guards).ensureRoomTypeInHotelOrThrow(1L, 99L);
    when(reservationsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var req = new ApplyUpgradeRequest();
    req.setNewRoomTypeId(99L);

    assertThatCode(() -> service.applyUpgrade(1L, 100L, req))
        .doesNotThrowAnyException();

    assertThat(sampleRes.getUpgrade_status()).isEqualTo(UpgradeStatus.APPLIED);
    assertThat(sampleRes.getRoom_type_id()).isEqualTo(99L);
    verify(inventoryService).releaseRange(eq(1L), eq(10L), any(), any());
    verify(inventoryService).reserveRangeOrThrow(eq(1L), eq(99L), any(), any());
    verify(reservationsRepository).save(any(Reservations.class));
  }

  // ---------- checkIn ----------

  @Test
  void checkIn_ok() {
    sampleRes.setStatus(ReservationStatus.CONFIRMED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    when(statusService.changeStatus(any(Reservations.class), eq(ReservationStatus.CHECKED_IN), any(), any()))
        .thenAnswer(inv -> {
          Reservations r = inv.getArgument(0);
          r.setStatus(ReservationStatus.CHECKED_IN);
          return r;
        });

    var r = service.checkIn(1L, 100L);

    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
    verify(statusService).changeStatus(eq(sampleRes), eq(ReservationStatus.CHECKED_IN), isNull(), isNull());
  }

  @Test
  void checkIn_canceled_throw() {
    sampleRes.setStatus(ReservationStatus.CANCELED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    assertThatThrownBy(() -> service.checkIn(1L, 100L))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("cancelled");
    verify(statusService, never()).changeStatus(any(), any(), any(), any());
  }

  @Test
  void checkIn_idempotent() {
    sampleRes.setStatus(ReservationStatus.CHECKED_IN);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    var r = service.checkIn(1L, 100L);

    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
    verify(statusService, never()).changeStatus(any(), any(), any(), any());
    verify(reservationsRepository, never()).save(any());
  }

  // ---------- checkOut ----------

  @Test
  void checkOut_ok() {
    sampleRes.setStatus(ReservationStatus.CONFIRMED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    when(statusService.changeStatus(any(Reservations.class), eq(ReservationStatus.CHECKED_OUT), any(), any()))
        .thenAnswer(inv -> {
          Reservations r = inv.getArgument(0);
          r.setStatus(ReservationStatus.CHECKED_OUT);
          return r;
        });

    var r = service.checkOut(1L, 100L);

    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    verify(statusService).changeStatus(eq(sampleRes), eq(ReservationStatus.CHECKED_OUT), isNull(), isNull());
  }

  @Test
  void checkOut_canceled_throw() {
    sampleRes.setStatus(ReservationStatus.CANCELED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    assertThatThrownBy(() -> service.checkOut(1L, 100L))
        .isInstanceOf(BadRequestException.class);
    verify(statusService, never()).changeStatus(any(), any(), any(), any());
  }

  @Test
  void checkOut_idempotent() {
    sampleRes.setStatus(ReservationStatus.CHECKED_OUT);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    var r = service.checkOut(1L, 100L);

    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    verify(statusService, never()).changeStatus(any(), any(), any(), any());
    verify(reservationsRepository, never()).save(any());
  }

  // ---------- cancel ----------

  @Test
  void cancel_ok_delegatesToOrchestrator() {
    sampleRes.setStatus(ReservationStatus.CONFIRMED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    doAnswer(inv -> {
      Reservations r = inv.getArgument(0);
      r.setCanceled_at(LocalDateTime.now());
      return null;
    }).when(orchestrator).cancel(eq(sampleRes), eq("guest request"), isNull());

    service.cancel(1L, 100L, "guest request");

    assertThat(sampleRes.getCanceled_at()).isNotNull();
    verify(orchestrator).cancel(eq(sampleRes), eq("guest request"), isNull());
  }

  @Test
  void cancel_alreadyCanceled_noop_butStillDelegates() {
    sampleRes.setStatus(ReservationStatus.CANCELED);
    when(guards.getReservationInHotelOrThrow(1L, 100L)).thenReturn(sampleRes);

    doNothing().when(orchestrator).cancel(eq(sampleRes), eq("again"), isNull());

    service.cancel(1L, 100L, "again");

    verify(orchestrator).cancel(eq(sampleRes), eq("again"), isNull());
    verify(reservationsRepository, never()).save(any());
  }
}
