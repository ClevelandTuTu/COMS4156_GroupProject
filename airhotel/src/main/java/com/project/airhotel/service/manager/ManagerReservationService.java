package com.project.airhotel.service.manager;

import com.project.airhotel.dto.reservations.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservations.ReservationUpdateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationCoreService;
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
  private final EntityGuards entityGuards;
  private final ReservationCoreService core;

  public ManagerReservationService(ReservationsRepository reservationsRepository,
                                   EntityGuards entityGuards,
                                   ReservationCoreService core) {
    this.reservationsRepository = reservationsRepository;
    this.entityGuards = entityGuards;
    this.core = core;
  }

  public List<Reservations> listReservations(Long hotelId, ReservationStatus status,
                                             LocalDate start, LocalDate end) {
    entityGuards.ensureHotelExists(hotelId);
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
    return entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
  }

  public Reservations patchReservation(Long hotelId, Long reservationId, ReservationUpdateRequest req) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);

    if (req.getRoomTypeId() != null) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
      r.setRoom_type_id(req.getRoomTypeId());
    }
    if (req.getRoomId() != null) {
      entityGuards.ensureRoomBelongsToHotelAndType(hotelId, req.getRoomId(), r.getRoom_type_id());
      r.setRoom_id(req.getRoomId());
    }
    if (req.getCheckInDate() != null || req.getCheckOutDate() != null) {
      core.recalcNightsOrThrow(
          r,
          req.getCheckInDate() != null ? req.getCheckInDate() : r.getCheck_in_date(),
          req.getCheckOutDate() != null ? req.getCheckOutDate() : r.getCheck_out_date()
      );
    }
    if (req.getNumGuests() != null) r.setNum_guests(req.getNumGuests());
    if (req.getCurrency() != null) r.setCurrency(req.getCurrency());
    if (req.getPriceTotal() != null) r.setPrice_total(req.getPriceTotal());
    if (req.getNotes() != null) r.setNotes(req.getNotes());

    if (req.getStatus() != null) {
      r = core.changeStatus(r, req.getStatus(), null, null);
    }

    return reservationsRepository.save(r);
  }

  public Reservations applyUpgrade(Long hotelId, Long reservationId, ApplyUpgradeRequest req) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getNewRoomTypeId());

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
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_IN) {
      return r;
    }
    return core.changeStatus(r, ReservationStatus.CHECKED_IN, null, null);
  }

  public Reservations checkOut(Long hotelId, Long reservationId) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      return r;
    }
    return core.changeStatus(r, ReservationStatus.CHECKED_OUT, null, null);
  }

  public void cancel(Long hotelId, Long reservationId, String reason) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
    core.cancel(r, reason, null);
  }
}