package com.project.airhotel.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload to apply a room-type upgrade to an existing reservation. The
 * client must specify the target room type that belongs to the same hotel as
 * the current reservation. Validation of ownership and availability is
 * performed by the application/service layer.
 * Author: Ziyang Su Version: 1.0.0
 */
@Data
public class ApplyUpgradeRequest {

  /**
   * Target room type id to upgrade to. Must not be null. The room type is
   * expected to belong to the same hotel as the reservation being upgraded.
   */
  @NotNull
  private Long newRoomTypeId;

}
