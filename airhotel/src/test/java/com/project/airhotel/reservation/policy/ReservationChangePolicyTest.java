package com.project.airhotel.reservation.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ReservationChangePolicy#verifyOrThrow(ReservationChange)}
 * to ensure both permissive (manager) and restrictive (user) partitions behave
 * as documented.
 */
class ReservationChangePolicyTest {

  private final ReservationChangePolicy userPolicy = new UserReservationPolicy();
  private final ReservationChangePolicy managerPolicy = new ManagerReservationPolicy();

  @Test
  @DisplayName("User policy blocks room type, room assignment, and status changes")
  void userPolicy_rejectsPrivilegedChanges() {
    assertThrows(BadRequestException.class,
        () -> userPolicy.verifyOrThrow(ReservationChange.builder()
            .newRoomTypeId(1L)
            .build()));

    assertThrows(BadRequestException.class,
        () -> userPolicy.verifyOrThrow(ReservationChange.builder()
            .newRoomId(9L)
            .build()));

    assertThrows(BadRequestException.class,
        () -> userPolicy.verifyOrThrow(ReservationChange.builder()
            .newStatus(ReservationStatus.CONFIRMED)
            .build()));
  }

  @Test
  @DisplayName("User policy permits no-op change requests")
  void userPolicy_allowsNoopChange() {
    assertDoesNotThrow(() -> userPolicy.verifyOrThrow(ReservationChange.builder().build()));
  }

  @Test
  @DisplayName("Manager policy allows combined changes for room type, room assignment, and status")
  void managerPolicy_allowsAllChanges() {
    final ReservationChange change = ReservationChange.builder()
        .newRoomTypeId(2L)
        .newRoomId(3L)
        .newStatus(ReservationStatus.CHECKED_IN)
        .build();

    assertDoesNotThrow(() -> managerPolicy.verifyOrThrow(change));
  }

  @Test
  @DisplayName("Error messages are specific to the disallowed field")
  void userPolicy_errorMessagesAreSpecific() {
    final BadRequestException ex = assertThrows(BadRequestException.class,
        () -> userPolicy.verifyOrThrow(ReservationChange.builder()
            .newStatus(ReservationStatus.CANCELED)
            .build()));

    assertTrue(ex.getMessage().contains("status"));
  }
}
