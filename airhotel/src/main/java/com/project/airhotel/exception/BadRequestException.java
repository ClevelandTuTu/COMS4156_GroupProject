package com.project.airhotel.exception;


/**
 * @author Ziyang Su
 * @version 1.0.0
 */
public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}
