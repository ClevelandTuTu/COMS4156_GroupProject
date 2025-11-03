package com.project.airhotel.service.user;

import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.exception.NotFoundException;
import com.project.airhotel.mapper.ReservationMapper;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.reservation.ReservationChangeAdapter;
import com.project.airhotel.reservation.UserReservationPolicy;
import com.project.airhotel.service.core.ReservationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class UserReservationService {

  /**
   * Repository to query and persist user reservations.
   */
  private final ReservationsRepository reservationsRepository;
  /**
   * Orchestrator for compound flows such as cancelation.
   */
  private final ReservationOrchestrator orchestrator;
  /**
   * Mapper from Reservations entities to API response DTOs.
   */
  private final ReservationMapper mapper;

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
   * Creates a reservation for the current user. Flow: - Guards ensure hotel and
   * room type are valid and related - Validates number of guests and optional
   * price total input - Sets dates and nights via nights service - Reserves
   * inventory for the stay range - Persists the reservation and returns a
   * detail DTO
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
  public ReservationDetailResponse createReservation(
      final Long userId,
      final CreateReservationRequest req) {
    final Reservations saved = orchestrator.createReservation(userId, req);
    return mapper.toDetail(saved);
  }

  /**
   * Partially updates a reservation owned by the user. Supported updates: -
   * Date changes: release old inventory, recalc nights, reserve new inventory -
   * Number of guests: must be positive
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
  public ReservationDetailResponse patchMyReservation(
      final Long userId,
      final Long id,
      final PatchReservationRequest req) {

    final Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));

    final var change = ReservationChangeAdapter.fromUserDto(req);

    final var updated = orchestrator.modifyReservation(r.getHotelId(), r,
        change, new UserReservationPolicy());
    return mapper.toDetail(updated);
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
  public void cancelMyReservation(final Long userId, final Long id) {
    final Reservations r =
        reservationsRepository.findByIdAndUserId(id, userId)
            .orElseThrow(()
                -> new NotFoundException("Reservation not found: " + id));
    orchestrator.cancel(r, "user-cancel", userId);
  }
}
