package com.project.airhotel.room.dto;

import com.project.airhotel.room.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for creating a room under a specific hotel. The service layer
 * validates hotel and room-type ownership, enforces per-hotel room-number
 * uniqueness, and may default the status when not provided.
 * Author: Ziyang Su Version: 1.0.0
 */
@Data
public class RoomsCreateRequest {

  /**
   * Target room-type identifier. Must not be null and must belong to the same
   * hotel where the room is created.
   */
  @NotNull
  private Long roomTypeId;

  /**
   * Human-readable room number or code (e.g., "101A"). Optional; when provided
   * it must be unique within the hotel.
   */
  private String roomNumber;

  /**
   * Floor number where the room is located (optional).
   */
  private Integer floor;

  /**
   * Initial room status (optional). If null, the service may set a default such
   * as RoomStatus.available.
   */
  private RoomStatus status;
}
