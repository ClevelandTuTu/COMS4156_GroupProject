package com.project.airhotel.model;

import com.project.airhotel.model.enums.ReservationStatus;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations_status_history")
public class ReservationsStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long reservation_id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus from_status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus to_status;

  @Column(nullable = false)
  private LocalDateTime changed_at;

  private Long changed_by_user_id;
  private Long changed_by_client_id;

  @Column(columnDefinition = "TEXT")
  private String reason;
}
