package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.policy.ReservationChangePolicy;
import com.project.airhotel.reservation.repository.ReservationsRepository;
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
  /**
   * Handles day-by-day inventory adjustments.
   */
  private final ReservationInventoryService inventoryService;
  /**
   * Persists status changes and writes history entries.
   */
  private final ReservationStatusService statusService;

  private final ReservationNightsService nightsService;
  private final EntityGuards entityGuards;
  private final ReservationsRepository reservationsRepository;
  private final ReservationPricingService pricingService;

  @Transactional
  public Reservations modifyReservation(
      final Long hotelId,
      final Reservations r,
      final ReservationChange change,
      final ReservationChangePolicy policy
  ) {
    // 1) Boundary Check
    entityGuards.ensureHotelExists(hotelId);
    if (!r.getHotelId().equals(hotelId)) {
      throw new BadRequestException("This reservation does not belong to the " +
          "hotel.");
    }
    policy.verifyOrThrow(change);

    // 2) Existence Check
    if (change.newRoomTypeId() != null) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId,
          change.newRoomTypeId());
    }
    if (change.newRoomId() != null) {
      final Long expectedType = change.newRoomTypeId() != null ?
          change.newRoomTypeId() : r.getRoomTypeId();
      entityGuards.ensureRoomBelongsToHotelAndType(hotelId,
          change.newRoomId(), expectedType);
    }

    final Long oldTypeId = r.getRoomTypeId();
    final var oldIn = r.getCheckInDate();
    final var oldOut = r.getCheckOutDate();

    // 3) Calculate new date and type
    final var effIn = change.effectiveCheckIn(oldIn);
    final var effOut = change.effectiveCheckOut(oldOut);
    final var effTypeId = change.newRoomTypeId() != null ?
        change.newRoomTypeId() : oldTypeId;

    final boolean typeChanged =
        change.newRoomTypeId() != null && !change.newRoomTypeId().equals(oldTypeId);

    if (typeChanged && change.newRoomId() == null && r.getRoomId() != null) {
      entityGuards.ensureRoomBelongsToHotelAndType(
          hotelId,
          r.getRoomId(),
          effTypeId
      );
    }

    final boolean needDatesOrTypeChange =
        change.isChangingDates(oldIn, oldOut) || change.isChangingRoomType(oldTypeId);

    // 4) First recalculate the evening figures,
    // then perform a "net switch" of inventory,
    // and finally update the room types
    if (needDatesOrTypeChange) {
      if (effIn == null || effOut == null || !effOut.isAfter(effIn)) {
        throw new BadRequestException("Invalid stay date range.");
      }
      r.setRoomTypeId(effTypeId);
      nightsService.recalcNightsOrThrow(r, effIn, effOut);
      pricingService.recalcTotalPriceOrThrow(r);
      inventoryService.applyRangeChangeOrThrow(
          r.getHotelId(),
          oldTypeId, oldIn, oldOut,      // old
          effTypeId, effIn, effOut       // new
      );
    }

    // 5) Specific room assignment (subject to policy permission)
    if (change.newRoomId() != null) {
      r.setRoomId(change.newRoomId());
    }

    // 6) Scalar field
    if (change.newNumGuests() != null) {
      if (change.newNumGuests() <= 0) {
        throw new BadRequestException("numGuests must be positive.");
      }
      r.setNumGuests(change.newNumGuests());
    }
    if (change.newCurrency() != null) {
      r.setCurrency(change.newCurrency());
    }
    if (change.newPriceTotal() != null) {
      if (change.newPriceTotal().signum() < 0) {
        throw new BadRequestException("priceTotal must be non-negative.");
      }
      r.setPriceTotal(change.newPriceTotal());
    }
    if (change.newNotes() != null) {
      r.setNotes(change.newNotes());
    }

    // 7) State transition (where the strategy allows)
    if (change.newStatus() != null) {
      statusService.changeStatus(r, change.newStatus(), null, null);
    }

    // 8) Persistence
    return reservationsRepository.save(r);
  }

  @Transactional
  public Reservations createReservation(final Long userId,
                                        final CreateReservationRequest req) {
    // Boundary check
    entityGuards.ensureHotelExists(req.getHotelId());
    entityGuards.ensureRoomTypeInHotelOrThrow(req.getHotelId(),
        req.getRoomTypeId());
    if (req.getNumGuests() == null || req.getNumGuests() <= 0) {
      throw new BadRequestException("numGuests must be positive.");
    }
    if (req.getPriceTotal() != null && req.getPriceTotal().signum() < 0) {
      throw new BadRequestException("priceTotal must be non-negative.");
    }

    // Build the entity and calculate the number of nights
    final Reservations r = new Reservations();
    r.setUserId(userId);
    r.setHotelId(req.getHotelId());
    r.setRoomTypeId(req.getRoomTypeId());
    r.setNumGuests(req.getNumGuests());
    r.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
    r.setPriceTotal(null);

    nightsService.recalcNightsOrThrow(r, req.getCheckInDate(),
        req.getCheckOutDate());

    pricingService.recalcTotalPriceOrThrow(r);

    // Unified inventory: Empty -> new
    inventoryService.applyRangeChangeOrThrow(
        r.getHotelId(),
        /* old */ null, null, null,
        /* new */ r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate()
    );

    return reservationsRepository.save(r);
  }

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

    inventoryService.applyRangeChangeOrThrow(
        r.getHotelId(),
        /* old */ r.getRoomTypeId(), r.getCheckInDate(), r.getCheckOutDate(),
        /* new */ null, null, null);

    r.setCanceledAt(LocalDateTime.now());
    statusService.changeStatus(r, ReservationStatus.CANCELED, reason,
        changedByUserId);
  }
}
