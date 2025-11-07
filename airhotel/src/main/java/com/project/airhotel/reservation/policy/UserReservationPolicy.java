package com.project.airhotel.reservation.policy;

import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * @author Ziyang Su
 * @version 1.0.0
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
