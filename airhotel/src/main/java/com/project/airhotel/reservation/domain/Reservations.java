package com.project.airhotel.reservation.domain;

import com.project.airhotel.common.model.ModelConstants;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.domain.enums.UpgradeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reservation master including guest counts, dates, currency and upgrade
 * status. Some fields may be null until assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_res_client_src_code", columnNames = {
        "client_id", "source_reservation_code"})
})
public class Reservations {
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Owning client id if originated by a client integration.
   */
  private Long clientId;

  /**
   * User id of the guest if known.
   */
  private Long userId;

  /**
   * Hotel id for the reservation.
   */
  @Column(nullable = false)
  private Long hotelId;

  /**
   * Reserved room type id.
   */
  @Column(nullable = false)
  private Long roomTypeId;

  /**
   * Assigned room id, if allocated.
   */
  private Long roomId;

  /**
   * Workflow status of the reservation.
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private ReservationStatus status = ReservationStatus.PENDING;

  /**
   * Check-in date.
   */
  @Column(nullable = false)
  private LocalDate checkInDate;

  /**
   * Check-out date.
   */
  @Column(nullable = false)
  private LocalDate checkOutDate;

  /**
   * Number of nights for the stay.
   */
  @Column(nullable = false)
  private Integer nights;

  /**
   * Number of guests.
   */
  @Column(nullable = false)
  private Integer numGuests;

  /**
   * ISO 4217 currency code, 3 letters.
   */
  @Column(nullable = false, columnDefinition = "CHAR(3)")
  private String currency;

  /**
   * Total price in currency minor units scale 2.
   */
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal priceTotal;

  /**
   * Source reservation code provided by external system.
   */
  @Column(length = ModelConstants.LEN_100)
  private String sourceReservationCode;

  /**
   * Upgrade eligibility and status.
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private UpgradeStatus upgradeStatus = UpgradeStatus.NOT_ELIGIBLE;

  /**
   * Free-form notes for back office use.
   */
  @Column(columnDefinition = "text")
  private String notes;

  /**
   * Creation timestamp managed by Hibernate.
   */
  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /**
   * Timestamp when an upgrade was applied.
   */
  private LocalDateTime upgradedAt;

  /**
   * Timestamp when the reservation was canceled.
   */
  private LocalDateTime canceledAt;
}
