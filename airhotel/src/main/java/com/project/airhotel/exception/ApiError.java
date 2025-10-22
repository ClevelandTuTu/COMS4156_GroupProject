package com.project.airhotel.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Immutable error payload returned by REST endpoints when an exception
 * occurs. Provides an application error code, message, request path,
 * timestamp, optional request id, and optional detail list.
 *
 * @param code application or HTTP-like status code
 * @param message short human readable message
 * @param path request path that produced the error
 * @param timestamp time when this error object is created
 * @param requestId optional request identifier for tracing
 * @param details optional list of detail strings
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    int code,
    String message,
    String path,
    OffsetDateTime timestamp,
    String requestId,
    List<String> details
) {
  /**
   * Factory method to create an ApiError with a list of details. The timestamp
   * is set to the current time and requestId is null.
   *
   * @param code    HTTP-like application code
   * @param msg     short human-readable message
   * @param path    request path that produced the error
   * @param details optional list of detail strings
   * @return ApiError instance
   */
  public static ApiError of(final int code, final String msg,
                            final String path, final List<String> details) {
    return new ApiError(code, msg, path, OffsetDateTime.now(),
        null, details);
  }

  /**
   * Factory method to create an ApiError without details. The timestamp is set
   * to the current time and requestId is null.
   *
   * @param code HTTP-like application code
   * @param msg  short human-readable message
   * @param path request path that produced the error
   * @return ApiError instance
   */
  public static ApiError of(final int code, final String msg,
                            final String path) {
    return of(code, msg, path, null);
  }
}
