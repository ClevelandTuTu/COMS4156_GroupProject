package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service that persists reservation status changes and writes a corresponding
 * history record for auditing.
 */
@Service
@RequiredArgsConstructor
public class ReservationStatusService {

  /** JPA repository for reservations. */
  private final ReservationsRepository reservationsRepository;
  /** JPA repository for status history records. */
  private final ReservationsStatusHistoryRepository historyRepository;
  /** State machine that validates allowed transitions. */
  private final ReservationStatusMachine statusMachine;


  /**
   * Change the reservation status after validating the transition. Also append
   * a status history item with the timestamp, operator, and reason.
   *
   * @param r               reservation to update
   * @param to              target status
   * @param reason          optional reason for the change
   * @param changedByUserId id of the user who performs the change, can be null
   * @return the persisted reservation with the new status
   * @throws BadRequestException if the transition is illegal
   */
  @Transactional
  public Reservations changeStatus(final Reservations r,
                                   final ReservationStatus to,
                                   final String reason,
                                   final Long changedByUserId) {
    final ReservationStatus from = r.getStatus();
    if (!statusMachine.canTransit(from, to)) {
      throw new BadRequestException("Illegal status transition: " + from + " "
          + "-> " + to);
    }
    r.setStatus(to);
    final Reservations saved = reservationsRepository.save(r);

    final ReservationsStatusHistory h = new ReservationsStatusHistory();
    h.setReservationId(saved.getId());
    h.setFromStatus(from);
    h.setToStatus(to);
    h.setChangedAt(LocalDateTime.now());
    h.setChangedByUserId(changedByUserId);
    h.setReason(reason);
    historyRepository.save(h);

    return saved;
  }
}
