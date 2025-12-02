package com.project.airhotel.reservation.adapter;

import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationUpdateRequest;

/**
 * This class centralizes the mapping logic from incoming manager-facing and
 * user-facing reservation update requests to the immutable
 * value object used by the domain and application services.
 */
public class ReservationChangeAdapter {
  /**
   * Utility class; no instances should be created.
   */
  private ReservationChangeAdapter() {}

  /**
   * Manager requests are allowed to modify all editable reservation fields,
   * including room type, concrete room, dates, number of guests, currency,
   * total price and status.
   */
  public static ReservationChange fromManagerDto(final ReservationUpdateRequest req) {
    return ReservationChange.builder()
        .newRoomTypeId(req.getRoomTypeId())
        .newRoomId(req.getRoomId())
        .newCheckIn(req.getCheckInDate())
        .newCheckOut(req.getCheckOutDate())
        .newNumGuests(req.getNumGuests())
        .newCurrency(req.getCurrency())
        .newPriceTotal(req.getPriceTotal())
        .newNotes(req.getNotes())
        .newStatus(req.getStatus())
        .build();
  }

  /**
   * End users are only allowed to change a limited subset of fields,
   * currently check-in/check-out dates and number of guests. Other fields
   * such as price, room type and status remain under manager or system control.
   */
  public static ReservationChange fromUserDto(final PatchReservationRequest req) {
    return ReservationChange.builder()
        .newCheckIn(req.getCheckInDate())
        .newCheckOut(req.getCheckOutDate())
        .newNumGuests(req.getNumGuests())
        .build();
  }

}
