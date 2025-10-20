package com.project.airhotel.dto.reservation;

import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(name = "ReservationDetailResponse")
public class ReservationDetailResponse {
  private Long id;
  private ReservationStatus status;
  private UpgradeStatus upgradeStatus;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private Integer nights;
  private Integer numGuests;
  private String currency;
  private BigDecimal priceTotal;
  private String roomNumber; // can be null
  private Instant createdAt;

  private List<NightlyPriceItem> nightlyPrices; // TODO: 后续从表映射
  private List<StatusHistoryItem> statusHistory;

  @Data public static class NightlyPriceItem {
    private LocalDate date;
    private BigDecimal price;
  }
  @Data public static class StatusHistoryItem {
    private ReservationStatus status;
    private Instant changedAt;
  }
}