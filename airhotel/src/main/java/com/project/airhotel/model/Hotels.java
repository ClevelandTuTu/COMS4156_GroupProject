package com.project.airhotel.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "hotels")
public class Hotels {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=120)
  private String name;

  @Column(length=120)
  private String brand;

  @Column(nullable=false, length=200)
  private String address_line1;

  @Column(length=200)
  private String address_line2;

  @Column(nullable=false, length=120)
  private String city;

  @Column(length=120)
  private String state;

  @Column(nullable=false, length=120)
  private String country;

  @Column(length=20)
  private String postal_code;

  // DECIMAL(2,1)
  @Column(precision = 2, scale = 1)
  private BigDecimal star_rating;

  @CreationTimestamp
  @Column(nullable=false)
  private LocalDateTime created_at;

  @UpdateTimestamp
  @Column(nullable=false)
  private LocalDateTime updated_at;
}
