package com.project.airhotel.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * Command object for partially updating a reservation owned by the user.
 * Supports changing stay dates and number of guests. Field-level and
 * cross-field validations ensure inputs are consistent and meaningful.
 * <p>
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

  /**
   * Cross-field validation to ensure at least one change is present.
   *
   * @return true if any field is non-null; false otherwise
   */
  @Schema(hidden = true)
  @AssertTrue(message = "At least one change in the reservation should be "
      + "provided")
  public boolean atLeastOneField() {
    return checkInDate != null
        || checkOutDate != null
        || numGuests != null;
  }

  /**
   * Cross-field validation to ensure date order when both dates are provided.
   * If either date is missing, this rule passes to let single-date updates
   * proceed.
   *
   * @return true if dates are not both provided, or if checkOutDate is after
   * checkInDate
   */
  @Schema(hidden = true)
  @AssertTrue(message = "Check out date should be after check in date")
  public boolean isDateOrderValid() {
    if (checkInDate != null && checkOutDate != null) {
      return checkOutDate.isAfter(checkInDate);
    }
    return true;
  }
}
