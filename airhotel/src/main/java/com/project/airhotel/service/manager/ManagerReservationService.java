package com.project.airhotel.service.manager;

import com.project.airhotel.dto.reservations.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservations.ReservationUpdateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationInventoryService;
import com.project.airhotel.service.core.ReservationNightsService;
import com.project.airhotel.service.core.ReservationOrchestrator;
import com.project.airhotel.service.core.ReservationStatusService;
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
  private final ReservationNightsService nightsService;
  private final ReservationInventoryService inventoryService;
  private final ReservationStatusService statusService;
  private final ReservationOrchestrator orchestrator;

  public ManagerReservationService(final ReservationsRepository reservationsRepository,
                                   final EntityGuards entityGuards,
                                   final ReservationNightsService nightsService,
                                   final ReservationInventoryService inventoryService,
                                   final ReservationStatusService statusService,
                                   final ReservationOrchestrator orchestrator) {
    this.reservationsRepository = reservationsRepository;
    this.entityGuards = entityGuards;
    this.nightsService = nightsService;
    this.inventoryService = inventoryService;
    this.statusService = statusService;
    this.orchestrator = orchestrator;
  }

  public List<Reservations> listReservations(final Long hotelId,
                                             final ReservationStatus status,
                                             final LocalDate start,
                                             final LocalDate end) {
    entityGuards.ensureHotelExists(hotelId);
    final boolean hasDates = (start != null && end != null);
    if (status != null && hasDates) {
      return reservationsRepository.findByHotelIdAndStatusAndStayRange(hotelId,
          status, start, end);
    }
    if (hasDates) {
      return reservationsRepository.findByHotelIdAndStayRange(hotelId, start,
          end);
    }
    if (status != null) {
      return reservationsRepository.findByHotelIdAndStatus(hotelId, status);
    }
    return reservationsRepository.findByHotelId(hotelId);
  }

  public Reservations getReservation(final Long hotelId,
                                     final Long reservationId) {
    return entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
  }

  public Reservations patchReservation(final Long hotelId,
                                       final Long reservationId,
                                       final ReservationUpdateRequest req) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);

    final boolean needChangeDates =
        (req.getCheckInDate() != null || req.getCheckOutDate() != null);
    final boolean needChangeRoomType =
        (req.getRoomTypeId() != null && !req.getRoomTypeId().equals(
            r.getRoom_type_id()));

    if (needChangeRoomType) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
      // release inventory of old stayed range
      inventoryService.releaseRange(r.getHotel_id(), r.getRoom_type_id(),
          r.getCheck_in_date(), r.getCheck_out_date());
      // change room id
      r.setRoom_type_id(req.getRoomTypeId());
      // pre-occupy inventory of new stayed range
      inventoryService.reserveRangeOrThrow(r.getHotel_id(),
          r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());
    }

    if (req.getRoomId() != null) {
      entityGuards.ensureRoomBelongsToHotelAndType(hotelId, req.getRoomId(),
          r.getRoom_type_id());
      // Todo: ensure the room is not occupied
      r.setRoom_id(req.getRoomId());
    }

    if (needChangeDates) {
      inventoryService.releaseRange(r.getHotel_id(), r.getRoom_type_id(),
          r.getCheck_in_date(), r.getCheck_out_date());

      final var newCheckIn = req.getCheckInDate() != null
          ? req.getCheckInDate()
          : r.getCheck_in_date();
      final var newCheckOut = req.getCheckOutDate() != null ?
          req.getCheckOutDate() : r.getCheck_out_date();
      nightsService.recalcNightsOrThrow(r, newCheckIn, newCheckOut);

      inventoryService.reserveRangeOrThrow(r.getHotel_id(),
          r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());
    }

    if (req.getNumGuests() != null) {
      r.setNum_guests(req.getNumGuests());
    }
    if (req.getCurrency() != null) {
      r.setCurrency(req.getCurrency());
    }
    if (req.getPriceTotal() != null) {
      r.setPrice_total(req.getPriceTotal());
    }
    if (req.getNotes() != null) {
      r.setNotes(req.getNotes());
    }

    if (req.getStatus() != null) {
      r = statusService.changeStatus(r, req.getStatus(),
          null, null);
    }

    return reservationsRepository.save(r);
  }

  public Reservations applyUpgrade(final Long hotelId, final Long reservationId,
                                   final ApplyUpgradeRequest req) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getNewRoomTypeId());

    if (r.getUpgrade_status() != UpgradeStatus.ELIGIBLE
        && r.getUpgrade_status() != UpgradeStatus.APPLIED) {
      throw new BadRequestException("You cannot upgrade this reservation " +
          "because the status is "
          + r.getUpgrade_status());
    }

    inventoryService.releaseRange(r.getHotel_id(), r.getRoom_type_id(),
        r.getCheck_in_date(), r.getCheck_out_date());
    r.setRoom_type_id(req.getNewRoomTypeId());
    inventoryService.reserveRangeOrThrow(r.getHotel_id(), r.getRoom_type_id()
        , r.getCheck_in_date(), r.getCheck_out_date());
    r.setUpgrade_status(UpgradeStatus.APPLIED);
    r.setUpgraded_at(LocalDateTime.now());
    return reservationsRepository.save(r);
  }

  public Reservations checkIn(final Long hotelId, final Long reservationId) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_IN) {
      return r;
    }
    return statusService.changeStatus(r, ReservationStatus.CHECKED_IN,
        null, null);
  }

  public Reservations checkOut(final Long hotelId, final Long reservationId) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    if (r.getStatus() == ReservationStatus.CANCELED) {
      throw new BadRequestException("Reservation has already been cancelled.");
    }
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      return r;
    }
    return statusService.changeStatus(r, ReservationStatus.CHECKED_OUT,
        null, null);
  }

  public void cancel(final Long hotelId, final Long reservationId,
                     final String reason) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    orchestrator.cancel(r, reason, null);
  }
}
