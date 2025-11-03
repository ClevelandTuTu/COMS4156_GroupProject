package com.project.airhotel.reservation;

import com.project.airhotel.domain.ReservationChange;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationUpdateRequest;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
public class ReservationChangeAdapter {
  private ReservationChangeAdapter() {}

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

  public static ReservationChange fromUserDto(final PatchReservationRequest req) {
    return ReservationChange.builder()
        .newCheckIn(req.getCheckInDate())
        .newCheckOut(req.getCheckOutDate())
        .newNumGuests(req.getNumGuests())
        .build();
  }

}
