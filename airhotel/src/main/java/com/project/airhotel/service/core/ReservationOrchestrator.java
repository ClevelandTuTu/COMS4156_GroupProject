package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orchestrates multi-step reservation operations that must stay consistent,
 * such as canceling a booking while releasing inventory and recording status
 * history.
 */
@Service
@RequiredArgsConstructor
public class ReservationOrchestrator {
  /** Handles day-by-day inventory adjustments. */
  private final ReservationInventoryService inventoryService;
  /** Persists status changes and writes history entries. */
  private final ReservationStatusService statusService;

  /**
   * Cancel a reservation if allowed. This releases the pre-occupied inventory
   * for all nights, marks the cancellation timestamp, and persists a valid
   * status transition with audit information.
   *
   * @param r               reservation to cancel
   * @param reason          optional human-readable reason for auditing
   * @param changedByUserId user id who triggers the change, can be null for
   *                        system actions
   * @throws BadRequestException if the reservation is already checked out
   */
  @Transactional
  public void cancel(final Reservations r, final String reason,
                     final Long changedByUserId) {
    if (r.getStatus() == ReservationStatus.CANCELED) {
      return;
    }
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      throw new BadRequestException("Reservation already checked out and "
          + "cannot be cancelled now.");
    }

    inventoryService.releaseRange(
        r.getHotelId(), r.getRoomTypeId(), r.getCheckInDate(),
        r.getCheckOutDate()
    );

    r.setCanceledAt(LocalDateTime.now());
    statusService.changeStatus(r, ReservationStatus.CANCELED, reason,
        changedByUserId);
  }
}
