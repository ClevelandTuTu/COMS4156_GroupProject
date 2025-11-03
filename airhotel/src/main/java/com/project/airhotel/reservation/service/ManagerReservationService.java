package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.dto.ApplyUpgradeRequest;
import com.project.airhotel.reservation.dto.ReservationUpdateRequest;
import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.domain.enums.UpgradeStatus;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.reservation.policy.ManagerReservationPolicy;
import com.project.airhotel.reservation.adapter.ReservationChangeAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Manager-facing reservation application service. Coordinates guards, nights
 * calculation, inventory adjustments, status transitions, and orchestration for
 * cancelation. All methods run within a transactional boundary.
 */
@Service
@RequiredArgsConstructor
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
   * @throws NotFoundException if the hotel does
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
   * @throws NotFoundException   if reservation
   *                                                            or hotel is not
   *                                                            found
   * @throws BadRequestException if the
   *                                                            reservation does
   *                                                            not belong to
   *                                                            the hotel
   */
  public Reservations getReservation(final Long hotelId,
                                     final Long reservationId) {
    return entityGuards.getReservationInHotelOrThrow(hotelId, reservationId);
  }

  /**
   * Patches a reservation for a hotel manager. Supports: - Changing room type:
   * releases old inventory for the original dates, sets the new room type, and
   * reserves inventory for the same date range under the new room type. -
   * Changing room id: verifies room belongs to hotel and type, then sets it. -
   * Changing dates: releases old inventory, recalculates nights and updates
   * dates, then reserves inventory for the new date range. - Updating scalar
   * fields: numGuests, currency, priceTotal, notes. - Updating status: uses
   * status service to validate transition and write history.
   * <p>
   * Inventory operations are executed around date and room-type changes to
   * maintain consistency.
   *
   * @param hotelId       hotel to which the reservation must belong
   * @param reservationId reservation to patch
   * @param req           requested modifications
   * @return the persisted reservation after changes
   * @throws NotFoundException   if the
   *                                                            reservation or
   *                                                            hotel is not
   *                                                            found
   * @throws BadRequestException if any
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
    final Reservations r = entityGuards.getReservationInHotelOrThrow(hotelId,
        reservationId);

    final var change = ReservationChangeAdapter.fromManagerDto(req);

    return orchestrator.modifyReservation(hotelId, r, change,
        new ManagerReservationPolicy());
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
   * @throws NotFoundException if hotel or
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
      throw new BadRequestException("You cannot upgrade this reservation " +
          "because the status is " + r.getUpgradeStatus());
    }

    final var change = ReservationChange.builder()
        .newRoomTypeId(req.getNewRoomTypeId())
        .build();

    final Reservations updated = orchestrator.modifyReservation(
        hotelId, r, change, new ManagerReservationPolicy()
    );

    updated.setUpgradeStatus(UpgradeStatus.APPLIED);
    updated.setUpgradedAt(java.time.LocalDateTime.now());
    return reservationsRepository.save(updated);
  }

  /**
   * Checks in a reservation if it is not canceled and not already checked in.
   * Delegates the transition to the status service.
   *
   * @param hotelId       hotel that must own the reservation
   * @param reservationId reservation to check in
   * @return the reservation after the transition, or the original if already
   * checked in
   * @throws NotFoundException if hotel or
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
   * @throws NotFoundException if hotel or
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
   * @throws NotFoundException if hotel or
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
