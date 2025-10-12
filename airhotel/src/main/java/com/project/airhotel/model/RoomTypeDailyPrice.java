package com.project.airhotel.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "room_type_daily_price", uniqueConstraints = {
    @UniqueConstraint(name = "uq_price_hotel_type_date", columnNames = {"hotel_id","room_type_id","stay_date"})
})
public class RoomTypeDailyPrice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private Long hotel_id;

  @Column(nullable=false)
  private Long room_type_id;

  @Column(nullable=false)
  private LocalDate stay_date;

  @Column(nullable=false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(precision = 5, scale = 2)
  private BigDecimal occupancy_percentage;

  @Column(columnDefinition = "json")
  private String computed_from;

  @UpdateTimestamp
  @Column(nullable=false)
  private LocalDateTime updated_at;
}