package com.project.airhotel.model;

import com.project.airhotel.model.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "rooms", uniqueConstraints = {
    @UniqueConstraint(name = "uq_rooms_hotel_roomno", columnNames = {"hotel_id","room_number"})
})
public class Rooms {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private Long hotel_id;

  @Column(nullable=false)
  private Long room_type_id;

  @Column(nullable=false, length=20)
  private String room_number;

  private Integer floor;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false, length=20)
  private RoomStatus status = RoomStatus.available;

  @CreationTimestamp
  @Column(nullable=false)
  private LocalDateTime created_at;

  @UpdateTimestamp
  @Column(nullable=false)
  private LocalDateTime updated_at;
}