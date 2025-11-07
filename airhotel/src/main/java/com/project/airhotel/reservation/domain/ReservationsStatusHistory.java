package com.project.airhotel.reservation.domain;

import com.project.airhotel.common.model.ModelConstants;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit trail of reservation status transitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations_status_history")
public class ReservationsStatusHistory {
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Reservation id to which this history belongs.
   */
  @Column(nullable = false)
  private Long reservationId;

  /**
   * Previous status.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private ReservationStatus fromStatus;

  /**
   * New status after the transition.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private ReservationStatus toStatus;

  /**
   * Time when the change happened.
   */
  @Column(nullable = false)
  private LocalDateTime changedAt;

  /**
   * User id who triggered the change, if any.
   */
  private Long changedByUserId;

  /**
   * Client id who triggered the change, if any.
   */
  private Long changedByClientId;

  /**
   * Optional human readable reason for the change.
   */
  @Column(columnDefinition = "TEXT")
  private String reason;
}
