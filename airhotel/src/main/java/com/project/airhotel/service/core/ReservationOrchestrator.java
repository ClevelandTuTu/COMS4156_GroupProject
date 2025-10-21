package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ReservationOrchestrator {
  private final ReservationInventoryService inventoryService;
  private final ReservationStatusService statusService;

  /** Booking cancellation: release inventory -> record cancellation time -> status change */
  @Transactional
  public void cancel(Reservations r, String reason, Long changedByUserId) {
    if (r.getStatus() == ReservationStatus.CANCELED) return;
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      throw new BadRequestException("Reservation already checked out and cannot be cancelled now.");
    }

    inventoryService.releaseRange(
        r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date()
    );

    r.setCanceled_at(LocalDateTime.now());
    statusService.changeStatus(r, ReservationStatus.CANCELED, reason, changedByUserId);
  }
}
