package com.project.airhotel.model;

import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.model.enums.UpgradeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "reservations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_res_client_src_code", columnNames = {"client_id","source_reservation_code"})
})
public class Reservations {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long client_id;
  private Long user_id;

  @Column(nullable=false)
  private Long hotel_id;

  @Column(nullable=false)
  private Long room_type_id;

  private Long room_id;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false, length=20)
  private ReservationStatus status = ReservationStatus.PENDING;

  @Column(nullable=false)
  private LocalDate check_in_date;

  @Column(nullable=false)
  private LocalDate check_out_date;

  @Column(nullable=false)
  private Integer nights;

  @Column(nullable=false)
  private Integer num_guests;

  @Column(nullable=false, columnDefinition = "CHAR(3)")
  private String currency;

  @Column(nullable=false, precision = 12, scale = 2)
  private BigDecimal price_total;

  @Column(length=100)
  private String source_reservation_code;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false, length=20)
  private UpgradeStatus upgrade_status = UpgradeStatus.NOT_ELIGIBLE;

  @Column(columnDefinition = "text")
  private String notes;

  @CreationTimestamp
  @Column(nullable=false)
  private LocalDateTime created_at;

  private LocalDateTime upgraded_at;
  private LocalDateTime canceled_at;
}