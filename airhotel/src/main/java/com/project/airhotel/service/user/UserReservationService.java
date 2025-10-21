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

  public UserReservationService(ReservationsRepository reservationsRepository,
                                ReservationNightsService nightsService,
                                ReservationInventoryService inventoryService,
                                ReservationOrchestrator orchestrator,
                                ReservationMapper mapper,
                                EntityGuards entityGuards) {
    this.reservationsRepository = reservationsRepository;
    this.nightsService = nightsService;
    this.inventoryService = inventoryService;
    this.orchestrator = orchestrator;
    this.mapper = mapper;
    this.entityGuards = entityGuards;
  }

  public List<ReservationSummaryResponse> listMyReservations(Long userId) {
    List<Reservations> list = reservationsRepository.findByUserId(userId);
    return list.stream()
        .map(mapper::toSummary)
        .collect(java.util.stream.Collectors.toList()); // JDK 16+ 可用 .toList()
  }

  public ReservationDetailResponse getMyReservation(Long userId, Long id) {
    Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    return mapper.toDetail(r);
  }

  @Transactional
  public ReservationDetailResponse createReservation(Long userId, CreateReservationRequest req) {
    // TODO: 价格/库存校验与 nightlyPrices 构建可在 core 中扩展
    entityGuards.ensureHotelExists(req.getHotelId());
    entityGuards.ensureRoomTypeInHotelOrThrow(req.getHotelId(), req.getRoomTypeId());

    if (req.getNumGuests() == null || req.getNumGuests() <= 0) {
      throw new BadRequestException("numGuests must be positive.");
    }
    if (req.getPriceTotal() != null && req.getPriceTotal().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("priceTotal must be non-negative.");
    }

    Reservations r = new Reservations();
    r.setUser_id(userId);
    r.setHotel_id(req.getHotelId());
    r.setRoom_type_id(req.getRoomTypeId());
    r.setNum_guests(req.getNumGuests());
    r.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
    r.setPrice_total(req.getPriceTotal());

    // Calculate nights & write date
    nightsService.recalcNightsOrThrow(r, req.getCheckInDate(), req.getCheckOutDate());

    // inventory verification + deduction
    inventoryService.reserveRangeOrThrow(r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());

    // TODO: 计算并设置 price_total；这里先置 0
//    r.setPrice_total(java.math.BigDecimal.ZERO);
    Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public ReservationDetailResponse patchMyReservation(Long userId, Long id, PatchReservationRequest req) {
    Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));

    if (req.getCheckInDate() != null || req.getCheckOutDate() != null) {
      inventoryService.releaseRange(r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());
      var newCheckIn = req.getCheckInDate() != null ? req.getCheckInDate() : r.getCheck_in_date();
      var newCheckOut = req.getCheckOutDate() != null ? req.getCheckOutDate() : r.getCheck_out_date();
      nightsService.recalcNightsOrThrow(r, newCheckIn, newCheckOut);
      // TODO: 重新计算 nightlyPrices & price_total
      inventoryService.reserveRangeOrThrow(r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());
    }
    if (req.getNumGuests() != null) {
      if (req.getNumGuests() <= 0) {
        throw new BadRequestException("numGuests must be positive.");
      }
      r.setNum_guests(req.getNumGuests());
    }
    Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public void cancelMyReservation(Long userId, Long id) {
    Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    orchestrator.cancel(r, "user-cancel", userId);
  }
}