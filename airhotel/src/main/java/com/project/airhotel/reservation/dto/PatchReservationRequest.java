package com.project.airhotel.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Data;

/**
 * Command object for partially updating a reservation owned by the user.
 * Supports changing stay dates and number of guests. Field-level and
 * cross-field validations ensure inputs are consistent and meaningful.
 * Validation summary:
 * - At least one field must be provided (check-in, check-out, or numGuests)
 * - checkInDate must be today or in the future when provided
 * - checkOutDate must be in the future when provided
 * - If both dates are provided, checkOutDate must be strictly after
 * checkInDate
 * - numGuests, when provided, must be at least 1
 */
@Data
@Schema(name = "PatchReservationRequest", description = "update a reservation"
    + " information")
public class PatchReservationRequest {

  /**
   * New check-in date in ISO yyyy-MM-dd. Must be present or future when
   * provided.
   */
  @Schema(description = "check in date of the reservation", example = "2026"
      + "-04-03")
  @FutureOrPresent(message = "check in date should be present or future")
  private LocalDate checkInDate;

  /**
   * New check-out date in ISO yyyy-MM-dd. Must be in the future when provided.
   */
  @Schema(description = "check out date of the reservation", example = "2026"
      + "-04-05")
  @Future(message = "check out date should be in the future")
  private LocalDate checkOutDate;

  /**
   * New number of guests for the reservation. Must be at least 1 when
   * provided.
   */
  @Schema(description = "num of guest in this reservation", example = "2")
  @Min(value = 1, message = "there should be at least one guest for the "
      + "reservation")
  private Integer numGuests;

}
