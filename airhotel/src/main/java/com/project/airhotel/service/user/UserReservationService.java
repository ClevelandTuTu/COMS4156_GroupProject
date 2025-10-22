package com.project.airhotel.service.user;

import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.exception.NotFoundException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.mapper.ReservationMapper;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationInventoryService;
import com.project.airhotel.service.core.ReservationNightsService;
import com.project.airhotel.service.core.ReservationOrchestrator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * User-facing reservation application service. Provides list, detail, create,
 * update, and cancel operations for reservations owned by the authenticated
 * user. Write operations are transactional and coordinate nights calculation
 * and inventory adjustments as needed.
 * <p>
 * Author: Jing Wang Version: 1.0.0
 */
@Service
public class UserReservationService {

  /**
   * Repository to query and persist user reservations.
   */
  private final ReservationsRepository reservationsRepository;
  /**
   * Service that validates and sets stay dates and computed nights.
   */
  private final ReservationNightsService nightsService;
  /**
   * Service that reserves and releases room-type inventory for date ranges.
   */
  private final ReservationInventoryService inventoryService;
  /**
   * Orchestrator for compound flows such as cancelation.
   */
  private final ReservationOrchestrator orchestrator;
  /**
   * Mapper from Reservations entities to API response DTOs.
   */
  private final ReservationMapper mapper;
  /**
   * Guards that validate entity existence and ownership constraints.
   */
  private final EntityGuards entityGuards;

  /**
   * Constructs the user reservation service with required collaborators.
   *
   * @param reservationsRepo repository used to access Reservations
   * @param nightsServ       service that recalculates nights from date ranges
   * @param inventoryServ    service that adjusts inventory across ranges
   * @param orches           orchestrator for cancelation flow
   * @param map              mapper from Reservation entities to DTOs
   * @param eGuards          guards ensuring hotel and room-type constraints
   */
  public UserReservationService(
      final ReservationsRepository reservationsRepo,
      final ReservationNightsService nightsServ,
      final ReservationInventoryService inventoryServ,
      final ReservationOrchestrator orches,
      final ReservationMapper map,
      final EntityGuards eGuards) {
    this.reservationsRepository = reservationsRepo;
    this.nightsService = nightsServ;
    this.inventoryService = inventoryServ;
    this.orchestrator = orches;
    this.mapper = map;
    this.entityGuards = eGuards;
  }

  /**
   * Lists reservations that belong to the given user.
   *
   * @param userId id of the user who owns the reservations
   * @return list of reservation summaries for the user
   */
  public List<ReservationSummaryResponse> listMyReservations(
      final Long userId) {
    final List<Reservations> list = reservationsRepository.findByUserId(userId);
    return list.stream()
        .map(mapper::toSummary)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Retrieves a single reservation owned by the user.
   *
   * @param userId id of the reservation owner
   * @param id     reservation id
   * @return detailed reservation response
   * @throws NotFoundException if the reservation does not exist or is not owned
   *                           by the user
   */
  public ReservationDetailResponse getMyReservation(final Long userId,
                                                    final Long id) {
    final Reservations r =
        reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(()
            -> new NotFoundException("Reservation not found: "
            + id));
    return mapper.toDetail(r);
  }

  // note: refactor reservation creation and update to Inventory Service,
  //  because these code are used by manager reservation service

  /**
   * Creates a reservation for the current user. Flow:
   * - Guards ensure hotel and room type are valid and related
   * - Validates number of guests and optional price total input
   * - Sets dates and nights via nights service
   * - Reserves inventory for the stay range
   * - Persists the reservation and returns a detail DTO
   * <p>
   * Note: total price calculation is intended to be handled by domain logic in
   * the future; for now the provided priceTotal is accepted after validation.
   *
   * @param userId id of the reservation owner
   * @param req    creation request containing hotel, room type, dates, guests,
   *               currency and optional price
   * @return detailed reservation response for the newly created reservation
   * @throws com.project.airhotel.exception.NotFoundException if hotel or room
   *                                                          type is invalid
   * @throws BadRequestException                              if numGuests is
   *                                                          not positive or
   *                                                          priceTotal is
   *                                                          negative
   */
  @Transactional
  public ReservationDetailResponse createReservation(
      final Long userId,
      final CreateReservationRequest req) {
    // note: calculate total prices instead of take it from input
    entityGuards.ensureHotelExists(req.getHotelId());
    entityGuards.ensureRoomTypeInHotelOrThrow(req.getHotelId(),
        req.getRoomTypeId());

    if (req.getNumGuests() == null || req.getNumGuests() <= 0) {
      throw new BadRequestException("numGuests must be positive.");
    }
    if (req.getPriceTotal() != null && req.getPriceTotal().compareTo(
        BigDecimal.ZERO) < 0) {
      throw new BadRequestException("priceTotal must be non-negative.");
    }

    final Reservations r = new Reservations();
    r.setUser_id(userId);
    r.setHotel_id(req.getHotelId());
    r.setRoom_type_id(req.getRoomTypeId());
    r.setNum_guests(req.getNumGuests());
    r.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
    // note: do calculation rather than take from input
    r.setPrice_total(req.getPriceTotal());

    // Calculate nights & write date
    nightsService.recalcNightsOrThrow(r, req.getCheckInDate(),
        req.getCheckOutDate());

    // inventory verification + deduction
    inventoryService.reserveRangeOrThrow(r.getHotel_id(),
        r.getRoom_type_id(),
        r.getCheck_in_date(),
        r.getCheck_out_date());

    final Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  /**
   * Partially updates a reservation owned by the user. Supported updates:
   * - Date changes: release old inventory, recalc nights, reserve new
   * inventory
   * - Number of guests: must be positive
   * <p>
   * Price recalculation is a planned enhancement and not included here.
   *
   * @param userId id of the reservation owner
   * @param id     reservation id
   * @param req    patch request with optional fields
   * @return detailed reservation response after persistence
   * @throws NotFoundException   if reservation is not found for the user
   * @throws BadRequestException if numGuests is provided and not positive
   */
  @Transactional
  public ReservationDetailResponse patchMyReservation(
      final Long userId,
      final Long id,
      final PatchReservationRequest req) {
    final Reservations r =
        reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(()
            -> new NotFoundException("Reservation not found: " + id));

    if (req.getCheckInDate() != null || req.getCheckOutDate() != null) {
      inventoryService.releaseRange(r.getHotel_id(),
          r.getRoom_type_id(),
          r.getCheck_in_date(),
          r.getCheck_out_date());
      final var newCheckIn = req.getCheckInDate() != null
          ? req.getCheckInDate()
          : r.getCheck_in_date();
      final var newCheckOut = req.getCheckOutDate() != null
          ? req.getCheckOutDate()
          : r.getCheck_out_date();
      nightsService.recalcNightsOrThrow(r, newCheckIn, newCheckOut);
      // note: recalculate nightlyPrices & price_total
      inventoryService.reserveRangeOrThrow(r.getHotel_id(),
          r.getRoom_type_id(),
          r.getCheck_in_date(),
          r.getCheck_out_date());
    }
    if (req.getNumGuests() != null) {
      // note: check number of guest is lower than the capacity of the room type
      if (req.getNumGuests() <= 0) {
        throw new BadRequestException("numGuests must be positive.");
      }
      r.setNum_guests(req.getNumGuests());
    }
    final Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  /**
   * Cancels a reservation owned by the user. Delegates to the orchestrator
   * which releases inventory, records cancelation timestamp, and changes
   * status.
   *
   * @param userId id of the reservation owner
   * @param id     reservation id to cancel
   * @throws NotFoundException if the reservation is not found for the user
   */
  @Transactional
  public void cancelMyReservation(final Long userId, final Long id) {
    final Reservations r =
        reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(()
            -> new NotFoundException("Reservation not found: " + id));
    orchestrator.cancel(r, "user-cancel", userId);
  }
}
