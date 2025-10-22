package com.project.airhotel.model;

/**
 * Common model-layer constants used to avoid magic numbers in JPA annotations.
 */
public final class ModelConstants {
  private ModelConstants() {

  }

  /** Generic VARCHAR length 20. */
  public static final int LEN_20 = 20;
  /** Generic VARCHAR length 50. */
  public static final int LEN_50 = 50;
  /** Generic VARCHAR length 100. */
  public static final int LEN_100 = 100;
  /** Generic VARCHAR length 120. */
  public static final int LEN_120 = 120;
  /** Generic VARCHAR length 200. */
  public static final int LEN_200 = 200;

  /** Decimal(12,2) precision 12. */
  public static final int P12 = 12;
  /** Decimal(12,2) scale 2. */
  public static final int S2 = 2;

  /** Decimal(2,1) precision 2. */
  public static final int P2 = 2;
  /** Decimal(2,1) scale 1. */
  public static final int S1 = 1;

  /** Decimal(6,2) precision 6. */
  public static final int P6 = 6;

  /** Decimal(5,2) precision 5. */
  public static final int P5 = 5;
}
