package com.project.airhotel.reservation.service;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Finite state machine describing allowed reservation status transitions. Use
 * canTransit to verify whether a transition is permissible.
 */
@Component
public class ReservationStatusMachine {

  /** Adjacency map for legal transitions. */
  private final Map<ReservationStatus, Set<ReservationStatus>> allowed =
      new EnumMap<>(ReservationStatus.class);

  /**
   * Initialize the adjacency map that encodes legal transitions between
   * reservation statuses.
   */
  public ReservationStatusMachine() {
    allowed.put(ReservationStatus.PENDING,
        EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELED));
    allowed.put(ReservationStatus.CONFIRMED,
        EnumSet.of(ReservationStatus.CHECKED_IN, ReservationStatus.CANCELED));
    allowed.put(ReservationStatus.CHECKED_IN,
        EnumSet.of(ReservationStatus.CHECKED_OUT));
    allowed.put(ReservationStatus.CHECKED_OUT,
        EnumSet.noneOf(ReservationStatus.class));
    allowed.put(ReservationStatus.CANCELED,
        EnumSet.noneOf(ReservationStatus.class));
    allowed.put(ReservationStatus.NO_SHOW,
        EnumSet.noneOf(ReservationStatus.class));
  }

  /**
   * Check whether a transition from one status to another is allowed.
   *
   * @param from current status
   * @param to   target status
   * @return true if allowed, false otherwise
   */
  public boolean canTransit(final ReservationStatus from,
                            final ReservationStatus to) {
    final Set<ReservationStatus> next = allowed.get(from);
    return next != null && next.contains(to);
  }
}
