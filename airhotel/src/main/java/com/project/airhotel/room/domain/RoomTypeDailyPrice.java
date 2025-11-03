package com.project.airhotel.room.domain;

import com.project.airhotel.common.model.ModelConstants;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * Computed or configured daily price for a room type on a date.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "room_type_daily_price", uniqueConstraints = {
    @UniqueConstraint(name = "uq_price_hotel_type_date", columnNames = {
        "hotel_id", "room_type_id", "stay_date"})
})
public class RoomTypeDailyPrice {
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Hotel id.
   */
  @Column(nullable = false)
  private Long hotelId;

  /**
   * Room type id.
   */
  @Column(nullable = false)
  private Long roomTypeId;

  /**
   * Stay date for which the price applies.
   */
  @Column(nullable = false)
  private LocalDate stayDate;

  /**
   * Price amount for the date.
   */
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal price;

  /**
   * Occupancy percentage used for pricing decisions.
   */
  @Column(precision = ModelConstants.P5, scale = ModelConstants.S2)
  private BigDecimal occupancyPercentage;

  /**
   * JSON string describing how the price was computed.
   */
  @Column(columnDefinition = "json")
  private String computedFrom;

  /**
   * Update timestamp managed by Hibernate.
   */
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
