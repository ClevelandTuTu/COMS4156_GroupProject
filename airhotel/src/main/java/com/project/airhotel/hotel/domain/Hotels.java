package com.project.airhotel.hotel.domain;

import com.project.airhotel.common.model.ModelConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Hotel master record including address and star rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hotels")
public class Hotels {

  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Hotel name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String name;

  /**
   * Optional brand name.
   */
  @Column(length = ModelConstants.LEN_120)
  private String brand;

  /**
   * Street address line 1.
   */
  @Column(name = "address_line1", nullable = false, length =
      ModelConstants.LEN_200)
  private String addressLine1;

  /**
   * Street address line 2.
   */
  @Column(name = "address_line2", length = ModelConstants.LEN_200)
  private String addressLine2;

  /**
   * City name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String city;

  /**
   * State or province.
   */
  @Column(length = ModelConstants.LEN_120)
  private String state;

  /**
   * Country name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String country;

  /**
   * Postal or ZIP code.
   */
  @Column(name = "postal_code", length = ModelConstants.LEN_20)
  private String postalCode;

  /**
   * Star rating, stored as DECIMAL(2,1).
   */
  @Column(name = "star_rating", precision = ModelConstants.P2, scale =
      ModelConstants.S1)
  private BigDecimal starRating;

  /**
   * Creation timestamp managed by Hibernate.
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /**
   * Update timestamp managed by Hibernate.
   */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
