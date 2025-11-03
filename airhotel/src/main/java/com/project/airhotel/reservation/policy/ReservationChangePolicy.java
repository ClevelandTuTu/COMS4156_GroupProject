package com.project.airhotel.reservation.policy;

import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
public interface ReservationChangePolicy {
  boolean allowChangeRoomType();

  boolean allowAssignConcreteRoom();

  boolean allowStatusChangeTo(ReservationStatus to);

  default void verifyOrThrow(final ReservationChange change) {
    if (change.newRoomTypeId() != null && !allowChangeRoomType()) {
      throw new BadRequestException("Not allowed to change room type.");
    }
    if (change.newRoomId() != null && !allowAssignConcreteRoom()) {
      throw new BadRequestException("Not allowed to assign a concrete room.");
    }
    if (change.newStatus() != null && !allowStatusChangeTo(change.newStatus())) {
      throw new BadRequestException("Not allowed to change status to " + change.newStatus());
    }
  }
}
