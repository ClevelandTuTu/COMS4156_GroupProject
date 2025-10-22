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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "room_types", uniqueConstraints = {
    @UniqueConstraint(name = "uq_room_types_hotel_code", columnNames = {
        "hotel_id", "code"})
})
public class RoomTypes {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long hotel_id;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private Integer capacity;

  @Column(length = 50)
  private String bed_type;

  private Integer bed_num;

  @Column(precision = 6, scale = 2)
  private BigDecimal bed_size_m2;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal base_rate;

  private Integer ranking;

  @Column(nullable = false)
  private Integer total_rooms;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime created_at;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updated_at;
}
