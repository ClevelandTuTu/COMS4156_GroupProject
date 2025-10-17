package com.project.airhotel.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
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
  public static ApiError of(int code, String msg, String path, List<String> details) {
    return new ApiError(code, msg, path, OffsetDateTime.now(), null, details);
  }
  public static ApiError of(int code, String msg, String path) {
    return of(code, msg, path, null);
  }
}