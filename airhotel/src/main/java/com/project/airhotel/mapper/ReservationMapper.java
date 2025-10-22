package com.project.airhotel.mapper;

import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.model.Reservations;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * Maps Reservations entities to API response DTOs. Provides both summary and
 * detail projections used by user/manager controllers.
 * <p>
 * Mapping notes:
 * - created_at is converted to Instant in UTC (ZoneOffset.UTC).
 * - roomNumber is currently set to null in the detail DTO and can be populated
 * later if/when the association is available in the Reservations aggregate or
 * via a join.
 */
@Component
public class ReservationMapper {

  /**
   * Maps a Reservations entity to a lightweight summary DTO.
   * <p>
   * Fields mapped: id, status, upgradeStatus, checkInDate, checkOutDate,
   * nights, numGuests, priceTotal, createdAt (converted to UTC Instant when
   * non-null).
   *
   * @param r the Reservations entity to convert
   * @return a ReservationSummaryResponse populated from the entity
   */
  public ReservationSummaryResponse toSummary(final Reservations r) {
    final ReservationSummaryResponse dto = new ReservationSummaryResponse();
    dto.setId(r.getId());
    dto.setStatus(r.getStatus());
    dto.setUpgradeStatus(r.getUpgradeStatus());
    dto.setCheckInDate(r.getCheckInDate());
    dto.setCheckOutDate(r.getCheckOutDate());
    dto.setNights(r.getNights());
    dto.setNumGuests(r.getNumGuests());
    dto.setPriceTotal(r.getPriceTotal());
    if (r.getCreatedAt() != null) {
      dto.setCreatedAt(r.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
    return dto;
  }

  /**
   * Maps a Reservations entity to a detailed DTO.
   * <p>
   * Fields mapped: id, status, upgradeStatus, checkInDate, checkOutDate,
   * nights, numGuests, currency, priceTotal, roomNumber (currently null),
   * createdAt (UTC Instant when non-null).
   *
   * @param r the Reservations entity to convert
   * @return a ReservationDetailResponse populated from the entity
   */
  public ReservationDetailResponse toDetail(final Reservations r) {
    final ReservationDetailResponse dto = new ReservationDetailResponse();
    dto.setId(r.getId());
    dto.setStatus(r.getStatus());
    dto.setUpgradeStatus(r.getUpgradeStatus());
    dto.setCheckInDate(r.getCheckInDate());
    dto.setCheckOutDate(r.getCheckOutDate());
    dto.setNights(r.getNights());
    dto.setNumGuests(r.getNumGuests());
    dto.setCurrency(r.getCurrency());
    dto.setPriceTotal(r.getPriceTotal());
    dto.setRoomNumber(null);
    if (r.getCreatedAt() != null) {
      dto.setCreatedAt(r.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
    return dto;
  }
}
