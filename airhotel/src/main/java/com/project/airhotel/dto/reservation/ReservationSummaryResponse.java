package com.project.airhotel.dto.reservation;

import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Schema(name = "ReservationSummaryResponse")
public class ReservationSummaryResponse {
  private Long id;
  private ReservationStatus status;
  private UpgradeStatus upgradeStatus;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private Integer nights;
  private Integer numGuests;
  private BigDecimal priceTotal;
  private String hotelName;     // TODO: 如果需要可在 mapper 里补
  private String roomTypeName;  // TODO: 同上
  private Instant createdAt;
}