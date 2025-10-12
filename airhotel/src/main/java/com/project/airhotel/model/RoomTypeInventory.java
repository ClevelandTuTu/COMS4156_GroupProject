package com.project.airhotel.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "room_type_inventory", uniqueConstraints = {
    @UniqueConstraint(name = "uq_inv_hotel_type_date", columnNames = {"hotel_id","room_type_id","stay_date"})
})
public class RoomTypeInventory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false)
  private Long hotel_id;

  @Column(nullable=false)
  private Long room_type_id;

  @Column(nullable=false)
  private LocalDate stay_date;

  @Column(nullable=false)
  private Integer total;

  @Column(nullable=false)
  private Integer reserved;

  @Column(nullable=false)
  private Integer blocked;

  @Column(nullable=false)
  private Integer available;

  @UpdateTimestamp
  @Column(nullable=false)
  private LocalDateTime updated_at;
}