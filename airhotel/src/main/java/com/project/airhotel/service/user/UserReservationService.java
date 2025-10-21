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

@Service
public class UserReservationService {

  private final ReservationsRepository reservationsRepository;
  private final ReservationNightsService nightsService;
  private final ReservationInventoryService inventoryService;
  private final ReservationOrchestrator orchestrator;
  private final ReservationMapper mapper;
  private final EntityGuards entityGuards;

  public UserReservationService(
      final ReservationsRepository reservationsRepository,
      final ReservationNightsService nightsService,
      final ReservationInventoryService inventoryService,
      final ReservationOrchestrator orchestrator,
      final ReservationMapper mapper,
      final EntityGuards entityGuards) {
    this.reservationsRepository = reservationsRepository;
    this.nightsService = nightsService;
    this.inventoryService = inventoryService;
    this.orchestrator = orchestrator;
    this.mapper = mapper;
    this.entityGuards = entityGuards;
  }

  public List<ReservationSummaryResponse> listMyReservations(
      final Long userId) {
    final List<Reservations> list = reservationsRepository.findByUserId(userId);
    return list.stream()
        .map(mapper::toSummary)
        .collect(java.util.stream.Collectors.toList());
  }

  public ReservationDetailResponse getMyReservation(final Long userId,
                                                    final Long id) {
    final Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: "
            + id));
    return mapper.toDetail(r);
  }

  // TODO: refactor reservation creation and update to Inventory Service,
  //  because these code are used by manager reservation service
  @Transactional
  public ReservationDetailResponse createReservation(
      final Long userId,
      final CreateReservationRequest req) {
    // TODO: calculate total prices instead of take it from input
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
    // TODO: do calculation rather than take from input
    r.setPrice_total(req.getPriceTotal());

    // Calculate nights & write date
    nightsService.recalcNightsOrThrow(r, req.getCheckInDate(),
        req.getCheckOutDate());

    // inventory verification + deduction
    inventoryService.reserveRangeOrThrow(r.getHotel_id(), r.getRoom_type_id()
        , r.getCheck_in_date(), r.getCheck_out_date());

    final Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public ReservationDetailResponse patchMyReservation(
      final Long userId,
      final Long id,
      final PatchReservationRequest req) {
    final Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException(
            "Reservation not found: " + id));

    if (req.getCheckInDate() != null || req.getCheckOutDate() != null) {
      inventoryService.releaseRange(r.getHotel_id(), r.getRoom_type_id(),
          r.getCheck_in_date(), r.getCheck_out_date());
      final var newCheckIn = req.getCheckInDate() != null
          ? req.getCheckInDate()
          : r.getCheck_in_date();
      final var newCheckOut = req.getCheckOutDate() != null
          ? req.getCheckOutDate()
          : r.getCheck_out_date();
      nightsService.recalcNightsOrThrow(r, newCheckIn, newCheckOut);
      // TODO: recalculate nightlyPrices & price_total
      inventoryService.reserveRangeOrThrow(r.getHotel_id(),
          r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());
    }
    if (req.getNumGuests() != null) {
      // TODO: check number of guest is lower than the capacity of the room type
      if (req.getNumGuests() <= 0) {
        throw new BadRequestException("numGuests must be positive.");
      }
      r.setNum_guests(req.getNumGuests());
    }
    final Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public void cancelMyReservation(final Long userId, final Long id) {
    final Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException(
            "Reservation not found: " + id));
    orchestrator.cancel(r, "user-cancel", userId);
  }
}
