package com.project.airhotel.model.enums;

/**
 * Reservation workflow statuses.
 */
public enum ReservationStatus {
  /** Created not yet confirmed. */
  PENDING,
  /** Confirmed and active. */
  CONFIRMED,
  /** Canceled prior to stay. */
  CANCELED,
  /** Guest has checked in. */
  CHECKED_IN,
  /** Guest has checked out. */
  CHECKED_OUT,
  /** Guest did not show up. */
  NO_SHOW
}
