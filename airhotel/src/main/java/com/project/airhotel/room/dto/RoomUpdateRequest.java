package com.project.airhotel.room.dto;

import com.project.airhotel.room.domain.enums.RoomStatus;
import lombok.Data;

/**
 * Request payload for partially updating a room. All fields are optional; only
 * non-null values are applied. Validation such as hotel/room-type ownership and
 * room-number uniqueness is enforced in the service layer.
 */
@Data
public class RoomUpdateRequest {

  /**
   * New room-type id; must belong to the same hotel if provided.
   */
  private Long roomTypeId;

  /**
   * New human-readable room number (e.g., "101A"); must be unique within the
   * hotel if provided.
   */
  private String roomNumber;

  /**
   * New floor number for the room, if provided.
   */
  private Integer floor;

  /**
   * New room status (e.g., available, maintenance), if provided.
   */
  private RoomStatus status;

}
