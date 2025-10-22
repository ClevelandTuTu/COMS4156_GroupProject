package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service that recalculates the number of nights for a reservation based on
 * check-in and check-out dates and updates the entity.
 */
@Service
public class ReservationNightsService {
  /**
   * Recalculate nights as the difference in epoch days and update the
   * reservation. The check-out date must be strictly later than the check-in
   * date.
   *
   * @param r        reservation entity to update
   * @param checkIn  check-in date, required
   * @param checkOut check-out date, required and must be after checkIn
   * @return the same reservation instance with updated dates and nights
   * @throws BadRequestException if dates are missing or invalid
   */
  public Reservations recalcNightsOrThrow(final Reservations r,
                                          final LocalDate checkIn,
                                          final LocalDate checkOut) {
    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("Check-in and check-out dates are "
          + "required.");
    }
    final int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    if (nights <= 0) {
      throw new BadRequestException("Check out date must be later than check "
          + "in date.");
    }
    r.setCheckInDate(checkIn);
    r.setCheckOutDate(checkOut);
    r.setNights(nights);
    return r;
  }
}
