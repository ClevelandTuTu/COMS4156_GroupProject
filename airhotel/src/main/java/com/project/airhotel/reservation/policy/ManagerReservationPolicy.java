package com.project.airhotel.reservation.policy;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
public class ManagerReservationPolicy implements ReservationChangePolicy{
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
