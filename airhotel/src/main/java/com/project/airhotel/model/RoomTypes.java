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


/**
 * Room type master data for a given hotel, including capacity, bedding
 * information and base pricing.
 */
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
  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Hotel id to which this room type belongs.
   */
  @Column(nullable = false)
  private Long hotelId;

  /**
   * Hotel-specific code that identifies the room type.
   */
  @Column(nullable = false, length = ModelConstants.LEN_50)
  private String code;

  /**
   * Human readable room type name.
   */
  @Column(nullable = false, length = ModelConstants.LEN_120)
  private String name;

  /**
   * Optional long description of the room type.
   */
  @Column(columnDefinition = "TEXT")
  private String description;

  /**
   * Maximum guest capacity for this room type.
   */
  @Column(nullable = false)
  private Integer capacity;

  /**
   * Bed type label, for example King, Queen, Twin.
   */
  @Column(length = ModelConstants.LEN_50)
  private String bedType;

  /**
   * Number of beds, if maintained.
   */
  private Integer bedNum;

  /**
   * Bed size in square meters if recorded.
   */
  @Column(name = "bed_size_m2", precision = ModelConstants.P6, scale =
      ModelConstants.S2)
  private BigDecimal bedSizeM2;

  /**
   * Base rate used as default price reference.
   */
  @Column(nullable = false, precision = ModelConstants.P12, scale =
      ModelConstants.S2)
  private BigDecimal baseRate;

  /**
   * Optional ranking used for ordering or merchandising.
   */
  private Integer ranking;

  /**
   * Total physical rooms of this type in the hotel.
   */
  @Column(nullable = false)
  private Integer totalRooms;

  /**
   * Creation timestamp managed by Hibernate.
   */
  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /**
   * Update timestamp managed by Hibernate.
   */
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
