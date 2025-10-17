package com.project.airhotel.dto.rooms;

import com.project.airhotel.model.enums.RoomStatus;
import lombok.Data;

@Data
public class RoomUpdateRequest {
  private Long roomTypeId;
  private String roomNumber;
  private Integer floor;
  private RoomStatus status;
}
