package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.reservation.adapter.ReservationChangeAdapter;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationDetailResponse;
import com.project.airhotel.reservation.dto.ReservationSummaryResponse;
import com.project.airhotel.reservation.mapper.ReservationMapper;
import com.project.airhotel.reservation.policy.UserReservationPolicy;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * User-facing reservation application service. Provides list, detail, create,
 * update, and cancel operations for reservations owned by the authenticated
 * user. Write operations are transactional and coordinate nights calculation
 * and inventory adjustments as needed.
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
   * Repository to load hotel names.
   */
  private final HotelsRepository hotelsRepository;
  /**
   * Repository to load room type names.
   */
  private final RoomTypesRepository roomTypesRepository;

  /**
   * Lists reservations that belong to the given user.
   *
   * @param userId id of the user who owns the reservations
   * @return list of reservation summaries for the user
   */
  public List<ReservationSummaryResponse> listMyReservations(
      final Long userId) {
    final List<Reservations> list = reservationsRepository.findByUserId(userId);
    final Map<Long, String> hotelNames = loadHotelNames(list);
    final Map<Long, String> roomTypeNames = loadRoomTypeNames(list);

    return list.stream()
        .map(r -> {
          final ReservationSummaryResponse dto = mapper.toSummary(r);
          dto.setHotelName(hotelNames.get(r.getHotelId()));
          dto.setRoomTypeName(roomTypeNames.get(r.getRoomTypeId()));
          return dto;
        })
        .collect(Collectors.toList());
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
   * Note: total price calculation is intended to be handled by domain logic in
   * the future; for now the provided priceTotal is accepted after validation.
   *
   * @param userId id of the reservation owner
   * @param req    creation request containing hotel, room type, dates, guests,
   *               currency and optional price
   * @return detailed reservation response for the newly created reservation
   * @throws NotFoundException if hotel or room
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

  private Map<Long, String> loadHotelNames(final List<Reservations> list) {
    final Set<Long> hotelIds = list.stream()
        .map(Reservations::getHotelId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (hotelIds.isEmpty()) {
      return Map.of();
    }
    return hotelsRepository.findAllById(hotelIds).stream()
        .collect(Collectors.toMap(Hotels::getId, Hotels::getName));
  }

  private Map<Long, String> loadRoomTypeNames(final List<Reservations> list) {
    final Set<Long> roomTypeIds = list.stream()
        .map(Reservations::getRoomTypeId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (roomTypeIds.isEmpty()) {
      return Map.of();
    }
    return roomTypesRepository.findAllById(roomTypeIds).stream()
        .collect(Collectors.toMap(RoomTypes::getId, RoomTypes::getName));
  }
}
