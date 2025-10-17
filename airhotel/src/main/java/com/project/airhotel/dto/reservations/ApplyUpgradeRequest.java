package com.project.airhotel.dto.reservations;

import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Data
public class ApplyUpgradeRequest {
  @NotNull
  private Long newRoomTypeId;
}
