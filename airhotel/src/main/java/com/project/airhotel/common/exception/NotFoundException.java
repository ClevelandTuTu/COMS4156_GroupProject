package com.project.airhotel.common.exception;

/**
 * Domain-level unchecked exception representing a 404 Not Found. It indicates
 * that the requested resource cannot be located.
 */
public class NotFoundException extends RuntimeException {
  /**
   * Construct a NotFoundException with a human-readable message.
   *
   * @param message explanation of the missing resource
   */
  public NotFoundException(final String message) {
    super(message);
  }
}
