package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReservationCoreService {

  private final ReservationsRepository reservationsRepository;
  private final ReservationsStatusHistoryRepository historyRepository;
  private final ReservationStatusMachine statusMachine;

  public ReservationCoreService(ReservationsRepository reservationsRepository,
                                ReservationsStatusHistoryRepository historyRepository,
                                ReservationStatusMachine statusMachine) {
    this.reservationsRepository = reservationsRepository;
    this.historyRepository = historyRepository;
    this.statusMachine = statusMachine;
  }

  /** 只做 nights 校验与写入；todo: 价格/库存后续可在此处扩展 */
  @Transactional
  public Reservations recalcNightsOrThrow(Reservations r, LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null) return r;
    int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    if (nights <= 0) throw new BadRequestException("Check out date must be later than check in date.");
    r.setCheck_in_date(checkIn);
    r.setCheck_out_date(checkOut);
    r.setNights(nights);
    return reservationsRepository.save(r);
  }

  /** Unified Status Change + History */
  @Transactional
  public Reservations changeStatus(Reservations r, ReservationStatus to, String reason, Long changedByUserId) {
    ReservationStatus from = r.getStatus();
    if (!statusMachine.canTransit(from, to)) {
      throw new BadRequestException("Illegal status transition: " + from + " -> " + to);
    }
    r.setStatus(to);
    System.out.println("from: " + from + " to: " + to);
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

  /** 取消（todo: 释放库存/退款等后续再补） */
  @Transactional
  public void cancel(Reservations r, String reason, Long changedByUserId) {
    if (r.getStatus() == ReservationStatus.CANCELED) return;
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      throw new BadRequestException("Reservation already checked out and cannot be cancelled now");
    }
    r.setCanceled_at(LocalDateTime.now());
    changeStatus(r, ReservationStatus.CANCELED, reason, changedByUserId);
  }
}