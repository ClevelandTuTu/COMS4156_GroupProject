package com.project.airhotel.dto.reservations;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Data
public class ApplyUpgradeRequest {
  @NotNull
  private Long newRoomTypeId;
}
