package com.project.airhotel.reservation.dto;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.domain.enums.UpgradeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

/**
 * Summary reservation response DTO.
 * A compact projection used in list views, containing key reservation fields
 * such as status, stay dates, nights, guests, and total price.
 */
@Data
@Schema(name = "ReservationSummaryResponse")
public class ReservationSummaryResponse {

  /** Reservation identifier. */
  private Long id;

  /** Current reservation status. */
  private ReservationStatus status;

  /** Upgrade workflow status. */
  private UpgradeStatus upgradeStatus;

  /** Check-in date (ISO yyyy-MM-dd). */
  private LocalDate checkInDate;

  /** Check-out date (ISO yyyy-MM-dd). */
  private LocalDate checkOutDate;

  /** Number of nights for the stay. */
  private Integer nights;

  /** Number of guests. */
  private Integer numGuests;

  /** Total price for the reservation. */
  private BigDecimal priceTotal;

  /** Hotel display name; may be populated by the mapper if needed. */
  private String hotelName;

  /** Room-type display name; may be populated by the mapper if needed. */
  private String roomTypeName;

  /** Creation timestamp of the reservation. */
  private Instant createdAt;

}
