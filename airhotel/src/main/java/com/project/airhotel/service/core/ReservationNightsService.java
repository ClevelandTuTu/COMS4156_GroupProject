package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
public class ReservationNightsService {
  // todo: 价格/库存后续可在此处扩展
  public Reservations recalcNightsOrThrow(Reservations r, LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("Check-in and check-out dates are required.");
    }
    int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    if (nights <= 0) {
      throw new BadRequestException("Check out date must be later than check in date.");
    }
    r.setCheck_in_date(checkIn);
    r.setCheck_out_date(checkOut);
    r.setNights(nights);
    return r;
  }
}
