package com.project.airhotel.reservation.dto;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.domain.enums.UpgradeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * Detailed reservation response DTO. Represents a single reservation including
 * current status, upgrade state, stay dates, nights, guest count, currency,
 * total price, assigned room number, creation timestamp, nightly price
 * breakdown, and status history.
 */
@Data
@Schema(name = "ReservationDetailResponse")
public class ReservationDetailResponse {

  /**
   * Reservation identifier.
   */
  private Long id;

  /**
   * Current reservation status.
   */
  private ReservationStatus status;

  /**
   * Upgrade workflow status for this reservation.
   */
  private UpgradeStatus upgradeStatus;

  /**
   * Check-in date in ISO yyyy-MM-dd.
   */
  private LocalDate checkInDate;

  /**
   * Check-out date in ISO yyyy-MM-dd.
   */
  private LocalDate checkOutDate;

  /**
   * Number of nights calculated from the stay dates.
   */
  private Integer nights;

  /**
   * Number of guests associated with the reservation.
   */
  private Integer numGuests;

  /**
   * 3-letter ISO currency code, for example USD.
   */
  private String currency;

  /**
   * Total price for the entire stay. to-do: do calculation in iteration 2
   */
  private BigDecimal priceTotal;

  /**
   * Assigned room number; may be null if not yet allocated.
   */
  private String roomNumber;

  /**
   * Creation timestamp of the reservation.
   */
  private Instant createdAt;

  /**
   * Nightly price breakdown for the stay. Note: this may be populated from a
   * separate table or pricing component in a future iteration.
   */
  private List<NightlyPriceItem> nightlyPrices;

  /**
   * Historical status changes with timestamps.
   */
  private List<StatusHistoryItem> statusHistory;

  /**
   * Single-night price item for the nightlyPrices list. Contains the date and
   * the price applied for that date.
   */
  @Data
  public static class NightlyPriceItem {

    /**
     * The stay date represented by this item (ISO yyyy-MM-dd).
     */
    private LocalDate date;

    /**
     * Price charged for the given night.
     */
    private BigDecimal price;
  }

  /**
   * Status history item capturing a past status and when it changed.
   */
  @Data
  public static class StatusHistoryItem {

    /**
     * Reservation status at that point in time.
     */
    private ReservationStatus status;

    /**
     * Timestamp when the status transition occurred.
     */
    private Instant changedAt;
  }
}
