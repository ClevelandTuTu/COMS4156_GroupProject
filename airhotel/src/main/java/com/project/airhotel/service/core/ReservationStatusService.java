package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ReservationStatusService {

  private final ReservationsRepository reservationsRepository;
  private final ReservationsStatusHistoryRepository historyRepository;
  private final ReservationStatusMachine statusMachine;


  public Reservations changeStatus(Reservations r, ReservationStatus to, String reason, Long changedByUserId) {
    ReservationStatus from = r.getStatus();
    if (!statusMachine.canTransit(from, to)) {
      throw new BadRequestException("Illegal status transition: " + from + " -> " + to);
    }
    r.setStatus(to);
    Reservations saved = reservationsRepository.save(r);

    ReservationsStatusHistory h = new ReservationsStatusHistory();
    h.setReservation_id(saved.getId());
    h.setFrom_status(from);
    h.setTo_status(to);
    h.setChanged_at(LocalDateTime.now());
    h.setChanged_by_user_id(changedByUserId);
    h.setReason(reason);
    historyRepository.save(h);

    return saved;
  }
}
