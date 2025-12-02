package com.project.airhotel.reservation.policy;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * Users cannot change room type, assign a concrete room,
 * or directly change reservation status.
 */
public class UserReservationPolicy implements ReservationChangePolicy {

  @Override
  public boolean allowChangeRoomType() {
    return false;
  }

  @Override
  public boolean allowAssignConcreteRoom() {
    return false;
  }

  @Override
  public boolean allowStatusChangeTo(final ReservationStatus to) {
    return false;
  }
}
