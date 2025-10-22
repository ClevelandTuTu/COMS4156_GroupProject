package com.project.airhotel.service.manager;

import com.project.airhotel.dto.reservation.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservation.ReservationUpdateRequest;
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
 * Manager-facing reservation application service. Coordinates guards, nights
 * calculation, inventory adjustments, status transitions, and orchestration for
 * cancelation. All methods run within a transactional boundary.
 */
@Service
@Transactional
public class ManagerReservationService {
  /**
   * Repository for persisting and querying reservations.
   */
  private final ReservationsRepository reservationsRepository;
  /**
   * Entity-boundary guards to ensure hotel, room type, room, and reservation
   * belong to the expected scope.
   */
  private final EntityGuards entityGuards;
  /**
   * Domain service that validates and writes stay dates and computed nights.
   */
  private final ReservationNightsService nightsService;
  /**
   * Domain service responsible for reserving and releasing room-type inventory
   * ranges.
   */
  private final ReservationInventoryService inventoryService;
  /**
   * Domain service for validating and executing reservation status transitions
   * with history.
   */
  private final ReservationStatusService statusService;
  /**
   * Orchestrator for higher-level flows such as cancelation (inventory release
   * then status change).
   */
  private final ReservationOrchestrator orchestrator;

  /**
   * Constructs the manager reservation service with all required
   * collaborators.
   *
   * @param reservationsRepo repository to read and write reservations
   * @param eGuards          guards that assert entity existence and ownership
   * @param nightsServ       service that recalculates nights from date ranges
   * @param inventoryServ    service that reserves/releases room-type inventory
   * @param statusServ       service that changes status and writes status
   *                         history
   * @param orches           orchestrator for compound flows (e.g., cancel)
   */
  public ManagerReservationService(
      final ReservationsRepository reservationsRepo,
      final EntityGuards eGuards,
      final ReservationNightsService nightsServ,
      final ReservationInventoryService inventoryServ,
      final ReservationStatusService statusServ,
      final ReservationOrchestrator orches) {
    this.reservationsRepository = reservationsRepo;
    this.entityGuards = eGuards;
    this.nightsService = nightsServ;
    this.inventoryService = inventoryServ;
    this.statusService = statusServ;
    this.orchestrator = orches;
  }

  /**
   * Lists reservations for a hotel with optional filters. If both status and
   * date range are provided, both filters apply. If only date range is
   * provided, the list is filtered by stay range. If only status is provided,
   * the list is filtered by status. If neither is provided, all reservations of
   * the hotel are returned.
   *
   * @param hotelId the hotel id to search under (must exist)
   * @param status  optional reservation status filter
   * @param start   optional start date of the stay range (inclusive)
   * @param end     optional end date of the stay range (exclusive)
   * @return list of reservations matching the filters
   * @throws com.project.airhotel.exception.NotFoundException if the hotel does
   *                                                          not exist
   */
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

  /**
   * Gets a single reservation by id, asserting it belongs to the given hotel.
   *
   * @param hotelId       the hotel id that must own the reservation
   * @param reservationId the reservation id
   * @return the reservation if it exists and belongs to the hotel
   * @throws com.project.airhotel.exception.NotFoundException   if reservation
   *                                                            or hotel is not
   *                                                            found
   * @throws com.project.airhotel.exception.BadRequestException if the
   *                                                            reservation does
   *                                                            not belong to
   *                                                            the hotel
   */
  public Reservations getReservation(final Long hotelId,
                                     final Long reservationId) {
    return entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
  }

  /**
   * Patches a reservation for a hotel manager. Supports:
   * - Changing room type: releases old inventory for the original dates, sets
   * the new room type, and reserves inventory for the same date range under the
   * new room type.
   * - Changing room id: verifies room belongs to hotel and type, then sets it.
   * - Changing dates: releases old inventory, recalculates nights and updates
   * dates, then reserves inventory for the new date range.
   * - Updating scalar fields: numGuests, currency, priceTotal, notes.
   * - Updating status: uses status service to validate transition and write
   * history.
   * <p>
   * Inventory operations are executed around date and room-type changes to
   * maintain consistency.
   *
   * @param hotelId       hotel to which the reservation must belong
   * @param reservationId reservation to patch
   * @param req           requested modifications
   * @return the persisted reservation after changes
   * @throws com.project.airhotel.exception.NotFoundException   if the
   *                                                            reservation or
   *                                                            hotel is not
   *                                                            found
   * @throws com.project.airhotel.exception.BadRequestException if any
   *                                                            ownership,
   *                                                            status
   *                                                            transition, or
   *                                                            validation rule
   *                                                            is violated
   */
  public Reservations patchReservation(
      final Long hotelId,
      final Long reservationId,
      final ReservationUpdateRequest req) {
    Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);

    final boolean needChangeDates =
        (req.getCheckInDate() != null || req.getCheckOutDate() != null);
    final boolean needChangeRoomType =
        (req.getRoomTypeId() != null && !req.getRoomTypeId().equals(
            r.getRoomTypeId()));

    if (needChangeRoomType) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
      // release inventory of old stayed range
      inventoryService.releaseRange(r.getHotelId(),
          r.getRoomTypeId(),
          r.getCheckInDate(),
          r.getCheckOutDate());
      // change room id
      r.setRoomTypeId(req.getRoomTypeId());
      // pre-occupy inventory of new stayed range
      inventoryService.reserveRangeOrThrow(r.getHotelId(),
          r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());
    }

    if (req.getRoomId() != null) {
      entityGuards.ensureRoomBelongsToHotelAndType(hotelId, req.getRoomId(),
          r.getRoomTypeId());
      // Todo: ensure the room is not occupied
      r.setRoomId(req.getRoomId());
    }

    if (needChangeDates) {
      inventoryService.releaseRange(r.getHotelId(), r.getRoomTypeId(),
          r.getCheckInDate(), r.getCheckOutDate());

      final var newCheckIn = req.getCheckInDate() != null
          ? req.getCheckInDate()
          : r.getCheckInDate();
      final var newCheckOut = req.getCheckOutDate() != null
          ? req.getCheckOutDate() : r.getCheckOutDate();
      nightsService.recalcNightsOrThrow(r, newCheckIn, newCheckOut);

      inventoryService.reserveRangeOrThrow(r.getHotelId(),
          r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate());
    }

    if (req.getNumGuests() != null) {
      r.setNumGuests(req.getNumGuests());
    }
    if (req.getCurrency() != null) {
      r.setCurrency(req.getCurrency());
    }
    if (req.getPriceTotal() != null) {
      r.setPriceTotal(req.getPriceTotal());
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

  /**
   * Applies a room-type upgrade for the reservation if upgrade status allows
   * it. Flow: release inventory for the current room type and dates, set the
   * new room type, reserve inventory for the same dates under the new type, set
   * upgrade status and timestamp, then persist the reservation.
   * <p>
   * Allowed upgrade statuses: ELIGIBLE and APPLIED.
   *
   * @param hotelId       hotel that must own the reservation
   * @param reservationId target reservation id
   * @param req           contains the new room type id
   * @return the saved reservation after upgrade
   * @throws com.project.airhotel.exception.NotFoundException if hotel or
   *                                                          reservation is not
   *                                                          found
   * @throws BadRequestException                              if the reservation
   *                                                          is not eligible
   *                                                          for upgrade
   */
  public Reservations applyUpgrade(final Long hotelId, final Long reservationId,
                                   final ApplyUpgradeRequest req) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getNewRoomTypeId());

    if (r.getUpgradeStatus() != UpgradeStatus.ELIGIBLE
        && r.getUpgradeStatus() != UpgradeStatus.APPLIED) {
      throw new BadRequestException("You cannot upgrade this reservation "
          + "because the status is "
          + r.getUpgradeStatus());
    }

    inventoryService.releaseRange(r.getHotelId(),
        r.getRoomTypeId(),
        r.getCheckInDate(),
        r.getCheckOutDate());
    r.setRoomTypeId(req.getNewRoomTypeId());
    inventoryService.reserveRangeOrThrow(r.getHotelId(),
        r.getRoomTypeId(),
        r.getCheckInDate(),
        r.getCheckOutDate());
    r.setUpgradeStatus(UpgradeStatus.APPLIED);
    r.setUpgradedAt(LocalDateTime.now());
    return reservationsRepository.save(r);
  }

  /**
   * Checks in a reservation if it is not canceled and not already checked in.
   * Delegates the transition to the status service.
   *
   * @param hotelId       hotel that must own the reservation
   * @param reservationId reservation to check in
   * @return the reservation after the transition, or the original if already
   * checked in
   * @throws com.project.airhotel.exception.NotFoundException if hotel or
   *                                                          reservation is not
   *                                                          found
   * @throws BadRequestException                              if the reservation
   *                                                          has been canceled
   */
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

  /**
   * Checks out a reservation if it is not canceled and not already checked out.
   * Delegates the transition to the status service.
   *
   * @param hotelId       hotel that must own the reservation
   * @param reservationId reservation to check out
   * @return the reservation after the transition, or the original if already
   * checked out
   * @throws com.project.airhotel.exception.NotFoundException if hotel or
   *                                                          reservation is not
   *                                                          found
   * @throws BadRequestException                              if the reservation
   *                                                          has been canceled
   */
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

  /**
   * Cancels a reservation by delegating to the orchestrator (which releases
   * inventory, records cancelation timestamp, and changes status).
   *
   * @param hotelId       hotel that must own the reservation
   * @param reservationId reservation to cancel
   * @param reason        optional textual reason that will be stored by the
   *                      orchestrator
   * @throws com.project.airhotel.exception.NotFoundException if hotel or
   *                                                          reservation is not
   *                                                          found
   */
  public void cancel(final Long hotelId, final Long reservationId,
                     final String reason) {
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);
    orchestrator.cancel(r, reason, null);
  }
}
