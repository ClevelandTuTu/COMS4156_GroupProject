package com.project.airhotel.dto.reservation;

//import com.project.airhotel.model.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(name = "PatchReservationRequest", description = "update a reservation information")
public class PatchReservationRequest {
//  @Schema(description = "room type ID", example = "1")
//  private Integer roomTypeId;

//  @Schema(description = "status of the reservation", example = "CONFIRMED")
//  private ReservationStatus status;

  @Schema(description = "check in date of the reservation", example = "2026-04-03")
  @FutureOrPresent(message = "check in date should be present or future")
  private LocalDate checkInDate;

  @Schema(description = "check out date of the reservation", example = "2026-04-05")
  @Future(message = "check out date should be in the future")
  private LocalDate checkOutDate;

  @Schema(description = "num of guest in this reservation", example="2")
  @Min(value = 1, message = "there should be at least one guest for the reservation")
  private Integer numGuests;

  @Schema(hidden = true)
  @AssertTrue(message = "At least one change in the reservation should be provided")
  public boolean atLeastOneField() {
    return checkInDate != null
        || checkOutDate != null
        || numGuests != null;
  }

  @Schema(hidden = true)
  @AssertTrue(message = "Check out date should be after check in date")
  public boolean isDateOrderValid() {
    if (checkInDate != null && checkOutDate != null) {
      return checkOutDate.isAfter(checkInDate);
    }
    return true;
  }
}
