package com.project.airhotel.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError handleNotFound(NotFoundException ex, HttpServletRequest req) {
    return ApiError.of(404, ex.getMessage(), req.getRequestURI());
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleBadRequest(BadRequestException ex, HttpServletRequest req) {
    return ApiError.of(400, ex.getMessage(), req.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .toList();
    String msg = details.isEmpty() ? "Validation failed" : details.getFirst();
    return ApiError.of(400, msg, req.getRequestURI(), details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + " " + v.getMessage())
        .toList();
    String msg = details.isEmpty() ? "Constraint violation" : details.getFirst();
    return ApiError.of(400, msg, req.getRequestURI(), details);
  }

  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleBindException(BindException ex, HttpServletRequest req) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .toList();
    String msg = details.isEmpty() ? "Bind failed" : details.getFirst();
    return ApiError.of(400, msg, req.getRequestURI(), details);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
    String msg = "Missing request parameter: " + ex.getParameterName();
    return ApiError.of(400, msg, req.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "required type";
    String msg = "Parameter '" + ex.getName() + "' type mismatch: expected " + required +
        (ex.getValue() != null ? (", but got '" + ex.getValue() + "'") : "");
    if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
      Object[] constants = ex.getRequiredType().getEnumConstants();
      String allowed = Arrays.stream(constants).map(Object::toString).collect(Collectors.joining(", "));
      msg += ". Allowed values: [" + allowed + "]";
    }
    return ApiError.of(400, msg, req.getRequestURI());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
    Throwable cause = ex.getMostSpecificCause();

    switch (cause) {
      case com.fasterxml.jackson.databind.exc.InvalidFormatException ife -> {
        var targetType = ife.getTargetType();
        String path = ife.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
        String msg = "Field '" + path + "' has invalid value '" + ife.getValue() + "'";
        if (targetType != null && targetType.isEnum()) {
          Object[] constants = targetType.getEnumConstants();
          String allowed = Arrays.stream(constants).map(Object::toString).collect(Collectors.joining(", "));
          msg += ". Allowed values: [" + allowed + "]";
        } else if (targetType != null) {
          msg += " for type " + targetType.getSimpleName();
        }
        return ApiError.of(400, msg, req.getRequestURI());
      }

      case java.time.format.DateTimeParseException dtpe -> {
        String msg = "Invalid date format: expected 'yyyy-MM-dd'. " + dtpe.getParsedString();
        return ApiError.of(400, msg, req.getRequestURI());
      }

      case com.fasterxml.jackson.databind.exc.MismatchedInputException mie -> {
        String path = mie.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
        String at = path.isEmpty() ? "" : (" at '" + path + "'");
        return ApiError.of(400, "Malformed JSON" + at + ": " + mie.getOriginalMessage(), req.getRequestURI());
      }
      default -> {
      }
    }

    return ApiError.of(400, "Malformed JSON request", req.getRequestURI());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiError handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
    String msg;

    Throwable root = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
    String lower = root.getMessage() != null ? root.getMessage().toLowerCase() : "";

    if (lower.contains("uq_res_client_src_code") || lower.contains("unique")) {
      msg = "Duplicate key violation: client_id + source_reservation_code must be unique";
    } else if (lower.contains("foreign key")) {
      msg = "Foreign key violation";
    } else {
      msg = root.getMessage();
    }
    return ApiError.of(409, msg, req.getRequestURI());
  }

  @ExceptionHandler(MissingPathVariableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleMissingPathVar(MissingPathVariableException ex, HttpServletRequest req) {
    return ApiError.of(400, "Missing path variable: " + ex.getVariableName(), req.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentConversionNotSupportedException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError handleConversionNotSupported(MethodArgumentConversionNotSupportedException ex, HttpServletRequest req) {
    String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "required type";
    String msg = "Type conversion not supported for parameter '" + ex.getName() + "': expected " + required;
    return ApiError.of(400, msg, req.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiError handleOthers(Exception ex, HttpServletRequest req) {
    return ApiError.of(500, "Internal Server Error", req.getRequestURI());
  }
}
