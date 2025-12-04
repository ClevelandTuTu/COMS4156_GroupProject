package com.project.airhotel.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.ReservationsStatusHistory;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.reservation.repository.ReservationsStatusHistoryRepository;
import com.project.airhotel.reservation.service.ReservationStatusService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test exercising ReservationStatusService + ReservationStatusMachine +
 * JPA repositories working together against an in-memory H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationStatusIntegrationTest {

  @Autowired
  private ReservationStatusService service;

  @Autowired
  private ReservationsRepository reservationsRepository;

  @Autowired
  private ReservationsStatusHistoryRepository historyRepository;

  private Reservations buildPendingReservation() {
    final Reservations r = new Reservations();
    r.setHotelId(1L);
    r.setRoomTypeId(10L);
    r.setCheckInDate(LocalDate.of(2030, 1, 1));
    r.setCheckOutDate(LocalDate.of(2030, 1, 3));
    r.setNights(2);
    r.setNumGuests(2);
    r.setCurrency("USD");
    r.setPriceTotal(new BigDecimal("250.00"));
    r.setStatus(ReservationStatus.PENDING);
    return reservationsRepository.save(r);
  }

  @Test
  @DisplayName("changeStatus persists reservation and writes history record with saved id")
  void changeStatus_persists_and_writes_history() {
    final Reservations pending = buildPendingReservation();

    final Reservations updated = service.changeStatus(
        pending,
        ReservationStatus.CONFIRMED,
        "integration-check",
        999L);

    // Reload reservation from DB to ensure persisted value
    final Reservations reloaded = reservationsRepository.findById(updated.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

    final List<ReservationsStatusHistory> history =
        historyRepository.findAll();
    assertThat(history).hasSize(1);
    final ReservationsStatusHistory h = history.getFirst();
    assertThat(h.getReservationId()).isEqualTo(updated.getId());
    assertThat(h.getFromStatus()).isEqualTo(ReservationStatus.PENDING);
    assertThat(h.getToStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    assertThat(h.getReason()).isEqualTo("integration-check");
    assertThat(h.getChangedByUserId()).isEqualTo(999L);
    assertThat(h.getChangedAt()).isNotNull();
  }
}
