package com.project.airhotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-night pricing breakdown for a reservation, including tax and discounts,
 * stored in the same currency as the reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations_nightly_prices", uniqueConstraints = {
    @UniqueConstraint(name = "uq_resnight_res_date", columnNames = {
        "reservation_id", "stay_date"})
})
public class ReservationsNightlyPrices {
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Owning reservation id.
   */
  @Column(nullable = false)
  private Long reservationId;

  /**
   * The night covered by this price row.
   */
  @Column(nullable = false)
  private LocalDate stayDate;

  /**
   * Room type id used for the price, if captured.
   */
  private Long roomTypeId;

  /**
   * Base price amount for the night.
   */
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal price;

  /**
   * Discount amount for the night.
   */
  @Builder.Default
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal discount = BigDecimal.ZERO;

  /**
   * Tax amount for the night.
   */
  @Builder.Default
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal tax = BigDecimal.ZERO;

  /**
   * ISO 4217 currency code, 3 letters.
   */
  @Column(nullable = false, columnDefinition = "CHAR(3)")
  private String currency;
}
