package com.project.airhotel.exception;


/**
 * Domain-level unchecked exception representing a 400 Bad Request. It indicates
 * that the client request is syntactically correct but semantically invalid for
 * the current operation.
 */
public class BadRequestException extends RuntimeException {
  /**
   * Construct a BadRequestException with a human-readable message.
   *
   * @param message explanation of the bad request condition
   */
  public BadRequestException(final String message) {
    super(message);
  }
}
