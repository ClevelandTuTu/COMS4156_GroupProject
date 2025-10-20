package com.project.airhotel.mapper;

import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.model.Reservations;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class ReservationMapper {

  public ReservationSummaryResponse toSummary(Reservations r) {
    ReservationSummaryResponse dto = new ReservationSummaryResponse();
    dto.setId(r.getId());
    dto.setStatus(r.getStatus());
    dto.setUpgradeStatus(r.getUpgrade_status());
    dto.setCheckInDate(r.getCheck_in_date());
    dto.setCheckOutDate(r.getCheck_out_date());
    dto.setNights(r.getNights());
    dto.setNumGuests(r.getNum_guests());
    dto.setPriceTotal(r.getPrice_total());
    if (r.getCreated_at() != null) {
      dto.setCreatedAt(r.getCreated_at().toInstant(ZoneOffset.UTC));
    }
    return dto;
  }

  public ReservationDetailResponse toDetail(Reservations r) {
    ReservationDetailResponse dto = new ReservationDetailResponse();
    dto.setId(r.getId());
    dto.setStatus(r.getStatus());
    dto.setUpgradeStatus(r.getUpgrade_status());
    dto.setCheckInDate(r.getCheck_in_date());
    dto.setCheckOutDate(r.getCheck_out_date());
    dto.setNights(r.getNights());
    dto.setNumGuests(r.getNum_guests());
    dto.setCurrency(r.getCurrency());
    dto.setPriceTotal(r.getPrice_total());
    dto.setRoomNumber(null);
    if (r.getCreated_at() != null) {
      dto.setCreatedAt(r.getCreated_at().toInstant(ZoneOffset.UTC));
    }
    // nightlyPrices/statusHistory 可在将来聚合查询后设置
    return dto;
  }
}