package com.project.airhotel.reservation.domain;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

/**
 * This class set the policy to determine the permission.
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
