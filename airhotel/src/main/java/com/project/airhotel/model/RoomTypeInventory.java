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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Inventory counts for a room type on a date. Available is derived as total
 * minus reserved minus blocked by business logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "room_type_inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uq_inv_hotel_type_date", columnNames = {
        "hotel_id", "room_type_id", "stay_date"})
})
public class RoomTypeInventory {
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
   * Date for which the counts apply.
   */
  @Column(nullable = false)
  private LocalDate stayDate;

  /**
   * Total physical rooms of this type.
   */
  @Column(nullable = false)
  private Integer total;

  /**
   * Count of reserved rooms for the date.
   */
  @Column(nullable = false)
  private Integer reserved;

  /**
   * Count of blocked rooms for the date.
   */
  @Column(nullable = false)
  private Integer blocked;

  /**
   * Computed available rooms for the date.
   */
  @Column(nullable = false)
  private Integer available;

  /**
   * Update timestamp managed by Hibernate.
   */
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
