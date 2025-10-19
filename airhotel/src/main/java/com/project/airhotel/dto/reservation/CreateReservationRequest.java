package com.project.airhotel.dto.reservation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(name = "CreateReservationRequest", description = "create a reservation")
public class CreateReservationRequest {
  @Schema(description = "hotel ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "hotelId cannot be null")
  private Long hotelId;

  @Schema(description = "room type ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "roomTypeId cannot be null")
  private Long roomTypeId;

  @Schema(description = "reservation check in date", example = "2025-11-01", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "checkInDate cannot be null")
  @FutureOrPresent(message = "checkInDate must be today or future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkInDate;

  @Schema(description = "reservation check out date", example = "2025-11-01", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "checkOutDate cannot be null")
  @Future(message = "checkOutDate must be in the future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkOutDate;

  @Schema(description = "num of guest expected for this reservation", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 1, message = "numGuests must be at least 1")
  private Integer numGuests;

  @Schema(description = "currency used to pay the invoice", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
  @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
  @NotNull(message = "currency cannot be null")
  private String currency;

  @Schema(description = "source reservation code if booked through third party", example = "87J2GF")
  private String sourceReservationCode;

  @AssertTrue(message = "checkOutDate must be after checkInDate")
  @Schema(hidden = true)
  public boolean isDateOrderValid() {
    if (checkInDate == null || checkOutDate == null) {
      return false;
    }
    return checkOutDate.isAfter(checkInDate);
  }
}
