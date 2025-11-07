package com.project.airhotel.reservation.domain.enums;

/**
 * Complimentary/paid upgrade workflow state.
 */
public enum UpgradeStatus {
  /** Guest not eligible for upgrade. */
  NOT_ELIGIBLE,
  /** Eligible but not yet queued or applied. */
  ELIGIBLE,
  /** Queued and awaiting processing. */
  QUEUED,
  /** Upgrade has been applied. */
  APPLIED,
  /** Guest/ops declined upgrade. */
  DECLINED
}
