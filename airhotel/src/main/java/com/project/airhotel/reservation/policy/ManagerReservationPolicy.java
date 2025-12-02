package com.project.airhotel.reservation.policy;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * Managers are allowed to change room type, assign a concrete room,
 * and change reservation status.
 */
public class ManagerReservationPolicy implements ReservationChangePolicy {

  @Override
  public boolean allowChangeRoomType() {
    return true;
  }

  @Override
  public boolean allowAssignConcreteRoom() {
    return true;
  }

  @Override
  public boolean allowStatusChangeTo(final ReservationStatus to) {
    return true;
  }
}
