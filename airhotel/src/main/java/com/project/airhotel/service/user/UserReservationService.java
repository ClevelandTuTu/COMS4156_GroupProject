package com.project.airhotel.service.user;

import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.exception.NotFoundException;
import com.project.airhotel.mapper.ReservationMapper;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.service.core.ReservationCoreService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserReservationService {

  private final ReservationsRepository reservationsRepository;
  private final ReservationCoreService core;
  private final ReservationMapper mapper;

  public UserReservationService(ReservationsRepository reservationsRepository,
                                ReservationCoreService core,
                                ReservationMapper mapper) {
    this.reservationsRepository = reservationsRepository;
    this.core = core;
    this.mapper = mapper;
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
    Reservations r = new Reservations();
    r.setUser_id(userId);
    r.setHotel_id(req.getHotelId());
    r.setRoom_type_id(req.getRoomTypeId());
    r.setNum_guests(req.getNumGuests());
    r.setCurrency(req.getCurrency() != null ? req.getCurrency() : "USD");
    core.recalcNightsOrThrow(r, req.getCheckInDate(), req.getCheckOutDate());
    // TODO: 计算并设置 price_total；这里先置 0
    r.setPrice_total(java.math.BigDecimal.ZERO);
    Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public ReservationDetailResponse patchMyReservation(Long userId, Long id, PatchReservationRequest req) {
    Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));

    if (req.getCheckInDate() != null || req.getCheckOutDate() != null) {
      core.recalcNightsOrThrow(r,
          req.getCheckInDate() != null ? req.getCheckInDate() : r.getCheck_in_date(),
          req.getCheckOutDate() != null ? req.getCheckOutDate() : r.getCheck_out_date());
      // TODO: 重新计算 nightlyPrices & price_total
    }
    if (req.getNumGuests() != null) {
      r.setNum_guests(req.getNumGuests());
    }
    Reservations saved = reservationsRepository.save(r);
    return mapper.toDetail(saved);
  }

  @Transactional
  public void cancelMyReservation(Long userId, Long id) {
    Reservations r = reservationsRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    core.cancel(r, "user-cancel", userId);
  }
}