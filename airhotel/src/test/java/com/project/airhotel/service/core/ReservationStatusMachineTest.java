package com.project.airhotel.service.core;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.service.ReservationStatusMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservationStatusMachineTest {

  @Test
  void canTransit_coversAllConfiguredBranches() {
    ReservationStatusMachine sm = new ReservationStatusMachine();

    // Allowed transitions
    assertTrue(sm.canTransit(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    assertTrue(sm.canTransit(ReservationStatus.PENDING, ReservationStatus.CANCELED));
    assertTrue(sm.canTransit(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN));
    assertTrue(sm.canTransit(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED));
    assertTrue(sm.canTransit(ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT));

    // Disallowed transitions
    assertFalse(sm.canTransit(ReservationStatus.PENDING, ReservationStatus.CHECKED_IN));
    assertFalse(sm.canTransit(ReservationStatus.CHECKED_OUT, ReservationStatus.PENDING));
    assertFalse(sm.canTransit(ReservationStatus.CANCELED, ReservationStatus.PENDING));
    assertFalse(sm.canTransit(ReservationStatus.NO_SHOW, ReservationStatus.CONFIRMED));
  }
}
