package com.project.airhotel.service;

import com.project.airhotel.dto.reservations.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservations.ReservationUpdateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.ManagerEntityGuards;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@Transactional
public class ManagerReservationService {
  private final ReservationsRepository reservationsRepository;
  private final ReservationsStatusHistoryRepository historyRepository;
  private final ManagerEntityGuards managerEntityGuards;

  public ManagerReservationService(ReservationsRepository reservationsRepository,
                                   ReservationsStatusHistoryRepository historyRepository,
                                   ManagerEntityGuards managerEntityGuards) {
    this.reservationsRepository = reservationsRepository;
    this.historyRepository = historyRepository;
    this.managerEntityGuards = managerEntityGuards;
  }

  public List<Reservations> listReservations(Long hotelId, ReservationStatus status,
                                             LocalDate start, LocalDate end) {
    managerEntityGuards.ensureHotelExists(hotelId);
    boolean hasDates = (start != null && end != null);
    if (status != null && hasDates) {
      return reservationsRepository.findByHotelIdAndStatusAndStayRange(hotelId, status, start, end);
    }
    if (hasDates) {
      return reservationsRepository.findByHotelIdAndStayRange(hotelId, start, end);
    }
    if (status != null) {
      return reservationsRepository.findByHotelIdAndStatus(hotelId, status);
    }
    return reservationsRepository.findByHotelId(hotelId);
  }

  public Reservations getReservation(Long hotelId, Long reservationId) {
    return managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
  }

  public Reservations patchReservation(Long hotelId, Long reservationId, ReservationUpdateRequest req) {
    Reservations r = managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);

    if (req.getRoomTypeId() != null) {
      managerEntityGuards.ensureRoomTypeExists(req.getRoomTypeId());
      r.setRoom_type_id(req.getRoomTypeId());
    }
    if (req.getRoomId() != null) {
      r.setRoom_id(req.getRoomId());
    }
    if (req.getCheckInDate() != null) {
      r.setCheck_in_date(req.getCheckInDate());
    }
    if (req.getCheckOutDate() != null) {
      r.setCheck_out_date(req.getCheckOutDate());
    }
    if (req.getNumGuests() != null) r.setNum_guests(req.getNumGuests());
    if (req.getCurrency() != null) r.setCurrency(req.getCurrency());
    if (req.getPriceTotal() != null) r.setPrice_total(req.getPriceTotal());
    if (req.getNotes() != null) r.setNotes(req.getNotes());

    if (r.getCheck_in_date() != null && r.getCheck_out_date() != null) {
      int nights = (int) (r.getCheck_out_date().toEpochDay() - r.getCheck_in_date().toEpochDay());
      if (nights <= 0) throw new BadRequestException("Check out date must be later than check in date.");
      r.setNights(nights);
    }

    if (req.getStatus() != null) {
      changeStatusWithHistory(r, req.getStatus(), null, null);
    }

    return reservationsRepository.save(r);
  }

  public Reservations applyUpgrade(Long hotelId, Long reservationId, ApplyUpgradeRequest req) {
    Reservations r = managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    managerEntityGuards.ensureRoomTypeExists(req.getNewRoomTypeId());

    if (r.getUpgrade_status() != UpgradeStatus.ELIGIBLE && r.getUpgrade_status() != UpgradeStatus.APPLIED) {
      throw new BadRequestException("You cannot upgrade this reservation because the status is "
          +  r.getUpgrade_status());
    }

    r.setRoom_type_id(req.getNewRoomTypeId());
    r.setUpgrade_status(UpgradeStatus.APPLIED);
    r.setUpgraded_at(LocalDateTime.now());
    return reservationsRepository.save(r);
  }

  public Reservations checkIn(Long hotelId, Long reservationId) {
    Reservations r = managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_IN) {
      return r;
    }
    changeStatusWithHistory(r, ReservationStatus.CHECKED_IN, null, null);
    return reservationsRepository.save(r);
  }

  public Reservations checkOut(Long hotelId, Long reservationId) {
    Reservations r = managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      return r;
    }
    changeStatusWithHistory(r, ReservationStatus.CHECKED_OUT, null, null);
    return reservationsRepository.save(r);
  }

  public void cancel(Long hotelId, Long reservationId, String reason) {
    Reservations r = managerEntityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) return;
    r.setCanceled_at(LocalDateTime.now());
    changeStatusWithHistory(r, ReservationStatus.CANCELED, reason, null);
    reservationsRepository.save(r);
  }

  private void changeStatusWithHistory(Reservations r, ReservationStatus to,
                                       String reason, Long changedByUserId) {
    ReservationStatus from = r.getStatus();
    r.setStatus(to);
    ReservationsStatusHistory h = new ReservationsStatusHistory();
    h.setReservation_id(r.getId());
    h.setFrom_status(from);
    h.setTo_status(to);
    h.setChanged_at(LocalDateTime.now());
    h.setChanged_by_user_id(changedByUserId);
    h.setReason(reason);
    historyRepository.save(h);
  }
}