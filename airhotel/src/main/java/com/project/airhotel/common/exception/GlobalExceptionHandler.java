package com.project.airhotel.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global REST exception handler that converts common exceptions into uniform ApiError responses
 * with proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle NotFoundException and return 404 ApiError.
   *
   * @param ex  the thrown NotFoundException
   * @param req current HTTP servlet request
   * @return ApiError with code 404 and message from the exception
   */
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(final NotFoundException ex,
      final HttpServletRequest req) {
    return ApiError.of(
        HttpStatus.NOT_FOUND.value(),
        ex.getMessage(),
        req.getRequestURI());
  }


  /**
   * Handle BadRequestException and return 400 ApiError.
   *
   * @param ex  the thrown BadRequestException
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and message from the exception
   */
  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleBadRequest(final BadRequestException ex,
      final HttpServletRequest req) {
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        req.getRequestURI());
  }

  /**
   * Handle bean validation errors raised during request body binding and return 400 ApiError with
   * field details.
   *
   * @param ex  MethodArgumentNotValidException with binding result
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and first field error as message, plus a list of error details
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMethodArgumentNotValid(
      final MethodArgumentNotValidException ex, final HttpServletRequest req) {
    final List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .toList();
    final String msg = details.isEmpty() ? "Validation failed"
        : details.getFirst();
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI(),
        details);
  }

  /**
   * Handle javax/jakarta ConstraintViolationException and return 400 ApiError with violation
   * details.
   *
   * @param ex  ConstraintViolationException containing violations
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and aggregated violation messages
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleConstraintViolation(
      final ConstraintViolationException ex,
      final HttpServletRequest req) {
    final List<String> details = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + " " + v.getMessage())
        .toList();
    final String msg = details.isEmpty() ? "Constraint violation"
        : details.getFirst();
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI(),
        details);
  }

  /**
   * Handle Spring BindException (e.g., form or query binding) and return 400 ApiError with field
   * details.
   *
   * @param ex  BindException with binding result
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and field error details
   */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleBindException(final BindException ex,
      final HttpServletRequest req) {
    final List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .toList();
    final String msg = details.isEmpty() ? "Bind failed" : details.getFirst();
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI(),
        details);
  }

  /**
   * Handle missing request parameter and return 400 ApiError.
   *
   * @param ex  MissingServletRequestParameterException with parameter info
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and missing parameter name
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMissingParam(
      final MissingServletRequestParameterException ex,
      final HttpServletRequest req) {
    final String msg = "Missing request parameter: " + ex.getParameterName();
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI());
  }

  /**
   * Handle argument type mismatch and return 400 ApiError. If the required type is an enum, allowed
   * values are included.
   *
   * @param ex  MethodArgumentTypeMismatchException with details
   * @param req current HTTP servlet request
   * @return ApiError with code 400 describing the mismatch
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleTypeMismatch(
      final MethodArgumentTypeMismatchException ex,
      final HttpServletRequest req) {
    final String required = ex.getRequiredType() != null
        ? ex.getRequiredType().getSimpleName() : "required type";
    String msg =
        "Parameter '"
            + ex.getName()
            + "' type mismatch: expected "
            + required
            + (ex.getValue() != null
            ? ", but got '" + ex.getValue() + "'"
            : "");
    if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
      final Object[] constants = ex.getRequiredType().getEnumConstants();
      final String allowed =
          Arrays.stream(constants)
              .map(Object::toString)
              .collect(Collectors.joining(", "));
      msg += ". Allowed values: [" + allowed + "]";
    }
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI());
  }

  /**
   * Handle unreadable HTTP message and try to produce a precise 400 ApiError based on the most
   * specific cause, including Jackson and date parsing errors.
   *
   * @param ex  HttpMessageNotReadableException with cause chain
   * @param req current HTTP servlet request
   * @return ApiError with code 400 and a human friendly message
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleNotReadable(final HttpMessageNotReadableException ex,
      final HttpServletRequest req) {
    final Throwable cause = ex.getMostSpecificCause();

    switch (cause) {
      case final com.fasterxml.jackson.databind.exc
          .InvalidFormatException ife -> {
        final var targetType = ife.getTargetType();
        final String path =
            ife.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .collect(Collectors.joining("."));
        String msg =
            "Field '" + path + "' has invalid value '" + ife.getValue() + "'";
        if (targetType != null && targetType.isEnum()) {
          final Object[] constants = targetType.getEnumConstants();
          final String allowed =
              Arrays.stream(constants).map(Object::toString)
                  .collect(Collectors.joining(", "));
          msg += ". Allowed values: [" + allowed + "]";
        } else if (targetType != null) {
          msg += " for type " + targetType.getSimpleName();
        }
        return ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            msg,
            req.getRequestURI());
      }

      case final java.time.format.DateTimeParseException dtpe -> {
        final String msg =
            "Invalid date format: expected 'yyyy-MM-dd'. "
                + dtpe.getParsedString();
        return ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            msg,
            req.getRequestURI());
      }

      case final com.fasterxml.jackson.databind.exc
          .MismatchedInputException mie -> {
        final String path =
            mie.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .collect(Collectors.joining("."));
        final String at = path.isEmpty() ? "" : (" at '" + path + "'");
        return ApiError.of(
            HttpStatus.BAD_REQUEST.value(),
            "Malformed JSON"
                + at
                + ": "
                + mie.getOriginalMessage(),
            req.getRequestURI());
      }
      default -> {
      }
    }

    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        "Malformed JSON request",
        req.getRequestURI());
  }

  /**
   * Handle database integrity issues (unique or foreign key) and return 409 ApiError. Message is
   * derived from the most specific cause.
   *
   * @param ex  DataIntegrityViolationException from Spring Data/JPA
   * @param req current HTTP servlet request
   * @return ApiError with code 409 and a concise explanation
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiError handleDataIntegrity(final DataIntegrityViolationException ex,
      final HttpServletRequest req) {
    final String msg;

    final Throwable root =
        org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
    final String lower = root.getMessage() != null
        ? root.getMessage().toLowerCase() : "";

    if (lower.contains("uq_res_client_src_code") || lower.contains("unique")) {
      msg = "Duplicate key violation: client_id + source_reservation_code "
          + "must be unique";
    } else if (lower.contains("foreign key")) {
      msg = "Foreign key violation";
    } else {
      msg = root.getMessage();
    }
    return ApiError.of(
        HttpStatus.CONFLICT.value(),
        msg,
        req.getRequestURI());
  }

  /**
   * Handle missing path variable and return 400 ApiError.
   *
   * @param ex  MissingPathVariableException with variable name
   * @param req current HTTP servlet request
   * @return ApiError with code 400 describing the missing variable
   */
  @ExceptionHandler(MissingPathVariableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMissingPathVar(final MissingPathVariableException ex,
      final HttpServletRequest req) {
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        "Missing path variable: " + ex.getVariableName(),
        req.getRequestURI());
  }

  /**
   * Handle MethodArgumentConversionNotSupportedException and return a 400 ApiError with expected
   * type information.
   *
   * @param ex  conversion not supported exception
   * @param req current HTTP servlet request
   * @return ApiError with code 400 describing the unsupported conversion
   */
  @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleConversionNotSupported(
      final MethodArgumentConversionNotSupportedException ex,
      final HttpServletRequest req) {
    final String required = ex.getRequiredType() != null
        ? ex.getRequiredType().getSimpleName() : "required type";
    final String msg =
        "Type conversion not supported for parameter '" + ex.getName() + "': "
            + "expected " + required;
    return ApiError.of(
        HttpStatus.BAD_REQUEST.value(),
        msg,
        req.getRequestURI());
  }


  /**
   * Fallback handler for any uncaught exception that should be mapped to a 500 Internal Server
   * Error.
   *
   * @param req current HTTP servlet request
   * @return ApiError with code 500 and a generic message
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError handleOthers(final HttpServletRequest req) {
    return ApiError.of(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        req.getRequestURI());
  }
}
