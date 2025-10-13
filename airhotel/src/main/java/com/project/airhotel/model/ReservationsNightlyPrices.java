package com.project.airhotel.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "reservations_nightly_prices", uniqueConstraints = {
    @UniqueConstraint(name = "uq_resnight_res_date", columnNames = {"reservation_id","stay_date"})
})
public class ReservationsNightlyPrices {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private Long reservation_id;

  @Column(nullable=false)
  private LocalDate stay_date;

  private Long room_type_id;

  @Column(nullable=false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(nullable=false, precision = 12, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(nullable=false, precision = 12, scale = 2)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(nullable=false, columnDefinition = "CHAR(3)")
  private String currency;
}