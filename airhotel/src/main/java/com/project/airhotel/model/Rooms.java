package com.project.airhotel.model;

import com.project.airhotel.model.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

/**
 * Physical room record within a hotel, including number and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms", uniqueConstraints = {
    @UniqueConstraint(name = "uq_rooms_hotel_roomno", columnNames = {
        "hotel_id", "room_number"})
})
public class Rooms {

  /**
   * Surrogate primary key.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Hotel id to which the room belongs.
   */
  @Column(nullable = false)
  private Long hotelId;

  /**
   * Room type id for this room.
   */
  @Column(nullable = false)
  private Long roomTypeId;

  /**
   * Human readable room number.
   */
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private String roomNumber;

  /**
   * Floor number, if maintained.
   */
  private Integer floor;

  /**
   * Operational status of the room.
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = ModelConstants.LEN_20)
  private RoomStatus status = RoomStatus.AVAILABLE;

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
