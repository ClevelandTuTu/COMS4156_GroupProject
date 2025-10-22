package com.project.airhotel.dto.reservation;

import com.project.airhotel.model.enums.ReservationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for manager-side partial updates to a reservation. All fields
 * are optional; only non-null fields are considered for update.
 * <p>
 * Semantics are enforced in the service layer (e.g., ownership checks,
 * inventory release and reserve around date or room-type changes, and status
 * transition rules).
 */
@Data

public class ReservationUpdateRequest {

  /**
   * New room type id. When provided, the room type must belong to the same
   * hotel.
   */
  private Long roomTypeId;

  /**
   * New concrete room id. When provided, must belong to the hotel and match the
   * room type.
   */
  private Long roomId;

  /**
   * New check-in date in ISO yyyy-MM-dd. Interacts with checkOutDate when both
   * are set.
   */
  private LocalDate checkInDate;

  /**
   * New check-out date in ISO yyyy-MM-dd. Must be after checkInDate when both
   * are set.
   */
  private LocalDate checkOutDate;

  /**
   * New number of guests for the reservation.
   */
  private Integer numGuests;

  /**
   * New 3-letter ISO currency code, for example USD.
   */
  private String currency;

  /**
   * New total price for the reservation.
   */
  private BigDecimal priceTotal;

  /**
   * Free-form notes associated with the reservation.
   */
  private String notes;

  /**
   * New reservation status. Transition validity is checked by the status
   * machine and recorded in status history by the service layer.
   */
  private ReservationStatus status;

}
