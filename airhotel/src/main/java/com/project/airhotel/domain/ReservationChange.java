package com.project.airhotel.domain;

import com.project.airhotel.model.enums.ReservationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Builder
public record ReservationChange(Long newRoomTypeId, Long newRoomId,
                                LocalDate newCheckIn, LocalDate newCheckOut,
                                Integer newNumGuests, String newCurrency,
                                BigDecimal newPriceTotal, String newNotes,
                                ReservationStatus newStatus) {

  public boolean isChangingDates(final LocalDate oldIn, final LocalDate oldOut) {
    return (newCheckIn != null && !newCheckIn.equals(oldIn))
        || (newCheckOut != null && !newCheckOut.equals(oldOut));
  }

  public boolean isChangingRoomType(final Long oldTypeId) {
    return newRoomTypeId != null && !newRoomTypeId.equals(oldTypeId);
  }

  public LocalDate effectiveCheckIn(final LocalDate oldIn) {
    return newCheckIn != null ? newCheckIn : oldIn;
  }

  public LocalDate effectiveCheckOut(final LocalDate oldOut) {
    return newCheckOut != null ? newCheckOut : oldOut;
  }
}
