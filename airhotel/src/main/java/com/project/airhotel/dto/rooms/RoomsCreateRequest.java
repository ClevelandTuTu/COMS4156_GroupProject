package com.project.airhotel.dto.rooms;

import com.project.airhotel.model.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Data
public class RoomsCreateRequest {
  @NotNull
  private Long roomTypeId;

  private String roomNumber;

  private Integer floor;

  private RoomStatus status;
}

