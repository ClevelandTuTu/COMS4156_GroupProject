package com.project.airhotel.room.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * Request payload for querying room-type availability within a stay window.
 */
@Data
public class RoomTypeAvailabilityRequest {

  @NotNull(message = "checkInDate is required")
  @FutureOrPresent(message = "checkInDate must be today or in the future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkInDate;

  @NotNull(message = "checkOutDate is required")
  @Future(message = "checkOutDate must be in the future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkOutDate;

  @Min(value = 1, message = "numGuests must be >= 1")
  private Integer numGuests;
}
