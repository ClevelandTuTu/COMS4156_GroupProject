package com.project.airhotel.roomtype.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * Availability projection for a room type within a stay window.
 */
@Data
@Builder
public class RoomTypeAvailabilityResponse {
  private Long roomTypeId;
  private String code;
  private String name;
  private String bedType;
  private Integer capacity;
  private Integer totalRooms;
  /**
   * Minimum available count across the requested stay dates.
   */
  private Integer available;
  /**
   * Base rate if present in the room type definition.
   */
  private BigDecimal baseRate;
}
