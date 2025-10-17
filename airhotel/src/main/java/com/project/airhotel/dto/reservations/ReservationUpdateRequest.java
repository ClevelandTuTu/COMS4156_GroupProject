package com.project.airhotel.dto.reservations;

import com.project.airhotel.model.enums.ReservationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Data

public class ReservationUpdateRequest {
  private Long roomTypeId;
  private Long roomId;


  private LocalDate checkInDate;
  private LocalDate checkOutDate;


  private Integer numGuests;
  private String currency;
  private BigDecimal priceTotal;
  private String notes;


  private ReservationStatus status;
}
