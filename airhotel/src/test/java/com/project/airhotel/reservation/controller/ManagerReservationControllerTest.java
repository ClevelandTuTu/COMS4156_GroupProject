package com.project.airhotel.reservation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.dto.ApplyUpgradeRequest;
import com.project.airhotel.reservation.dto.ReservationUpdateRequest;
import com.project.airhotel.reservation.service.ManagerReservationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ManagerReservationControllerTest {

  @Mock
  private ManagerReservationService reservationService;

  @InjectMocks
  private ManagerReservationController controller;

  @Test
  @DisplayName("list delegates filters to service")
  void list_withFilters() {
    final Reservations r = new Reservations();
    when(reservationService.listReservations(1L, ReservationStatus.CONFIRMED,
        LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 5)))
        .thenReturn(List.of(r));

    final List<Reservations> out = controller.list(
        1L,
        ReservationStatus.CONFIRMED,
        LocalDate.of(2025, 12, 1),
        LocalDate.of(2025, 12, 5));

    assertEquals(List.of(r), out);
  }

  @Test
  @DisplayName("get delegates to service")
  void get_delegates() {
    final Reservations r = new Reservations();
    when(reservationService.getReservation(2L, 9L)).thenReturn(r);

    final Reservations out = controller.get(2L, 9L);

    assertSame(r, out);
  }

  @Test
  @DisplayName("patch delegates with payload")
  void patch_delegates() {
    final Reservations r = new Reservations();
    final ReservationUpdateRequest req = new ReservationUpdateRequest();
    when(reservationService.patchReservation(3L, 10L, req)).thenReturn(r);

    final Reservations out = controller.patch(3L, 10L, req);

    assertSame(r, out);
  }

  @Test
  @DisplayName("applyUpgrade delegates with payload")
  void applyUpgrade_delegates() {
    final Reservations r = new Reservations();
    final ApplyUpgradeRequest req = new ApplyUpgradeRequest();
    when(reservationService.applyUpgrade(4L, 11L, req)).thenReturn(r);

    final Reservations out = controller.applyUpgrade(4L, 11L, req);

    assertSame(r, out);
  }

  @Test
  @DisplayName("checkIn delegates")
  void checkIn_delegates() {
    final Reservations r = new Reservations();
    when(reservationService.checkIn(5L, 12L)).thenReturn(r);

    final Reservations out = controller.checkIn(5L, 12L);

    assertSame(r, out);
  }

  @Test
  @DisplayName("checkOut delegates")
  void checkOut_delegates() {
    final Reservations r = new Reservations();
    when(reservationService.checkOut(6L, 13L)).thenReturn(r);

    final Reservations out = controller.checkOut(6L, 13L);

    assertSame(r, out);
  }

  @Test
  @DisplayName("cancel returns 204 and calls service")
  void cancel_returnsNoContent() {
    final ResponseEntity<Void> resp = controller.cancel(7L, 14L, "reason");

    assertEquals(204, resp.getStatusCodeValue());
    verify(reservationService).cancel(7L, 14L, "reason");
  }
}
