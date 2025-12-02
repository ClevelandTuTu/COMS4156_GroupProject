package com.project.airhotel.reservation.policy;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;

/**
 * Implementations decide whether a caller may change room type, assign a
 * concrete room, or set a reservation to a given status.
 */
public interface ReservationChangePolicy {
  boolean allowChangeRoomType();

  boolean allowAssignConcreteRoom();

  boolean allowStatusChangeTo(ReservationStatus to);

  /**
   * Validates against this policy and throws an exception
   * if any requested change is not permitted.
   *
   * @param change the requested reservation change
   * @throws BadRequestException if the change violates this policy
   */
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
