package com.project.airhotel.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Command object for creating a reservation. Includes hotel and room-type identifiers, stay dates,
 * guest count, currency, optional third-party reference, and a provisional total price. Validation
 * summary: - hotelId, roomTypeId, checkInDate, checkOutDate, currency, priceTotal are required -
 * checkInDate must be today or in the future - checkOutDate must be in the future and strictly
 * after checkInDate - currency must be a 3-letter uppercase ISO code - priceTotal must be
 * non-negative with up to 10 integer digits and 2 decimals - numGuests must be at least 1 when
 * provided
 */
@Data
@Schema(name = "CreateReservationRequest", description = "create a reservation")
public class CreateReservationRequest {

  /**
   * Maximum digits of the price.
   */
  private static final int PRICE_INTEGER_DIGITS = 10;

  /**
   * Target hotel identifier.
   */
  @Schema(description = "hotel ID", example = "1", requiredMode =
      Schema.RequiredMode.REQUIRED)
  @NotNull(message = "hotelId cannot be null")
  private Long hotelId;

  /**
   * Target room type identifier.
   */
  @Schema(description = "room type ID", example = "1", requiredMode =
      Schema.RequiredMode.REQUIRED)
  @NotNull(message = "roomTypeId cannot be null")
  private Long roomTypeId;

  /**
   * Check-in date in ISO format yyyy-MM-dd. Must be today or a future date.
   */
  @Schema(description = "reservation check in date", example = "2025-11-01",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "checkInDate cannot be null")
  @FutureOrPresent(message = "checkInDate must be today or future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkInDate;

  /**
   * Check-out date in ISO format yyyy-MM-dd. Must be a future date and strictly after checkInDate.
   */
  @Schema(description = "reservation check out date", example = "2025-11-01",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "checkOutDate cannot be null")
  @Future(message = "checkOutDate must be in the future")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate checkOutDate;

  /**
   * Number of guests. When provided, must be at least 1.
   */
  @Schema(description = "num of guest expected for this reservation",
      example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 1, message = "numGuests must be at least 1")
  private Integer numGuests;

  /**
   * Currency ISO 4217 code (3 uppercase letters), e.g. USD.
   */
  @Schema(description = "currency used to pay the invoice", example = "USD",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO "
      + "code")
  @NotNull(message = "currency cannot be null")
  private String currency;

  /**
   * Total price for the reservation. Non-negative, up to 10 integer digits and 2 decimals. Note:
   * may be recalculated by pricing logic on the server side.
   */
  @Schema(description = "total price of the reservation", example = "199.99",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "priceTotal cannot be null")
  @Digits(integer = PRICE_INTEGER_DIGITS, fraction = 2, message = "priceTotal"
      + " must be up to 10 integer digits and 2 decimals")
  @PositiveOrZero(message = "priceTotal must be >= 0")
  private BigDecimal priceTotal;

  /**
   * Optional upstream reservation code if this booking originated from a third party.
   */
  @Schema(description = "source reservation code if booked through third "
      + "party", example = "87J2GF")
  private String sourceReservationCode;
}
