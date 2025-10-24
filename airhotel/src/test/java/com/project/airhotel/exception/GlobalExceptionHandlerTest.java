package com.project.airhotel.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private HttpServletRequest req(final String uri) {
    return new MockHttpServletRequest("GET", uri);
  }

  @Test
  @DisplayName("NotFoundException → 404")
  void handleNotFound() {
    final var resp = handler.handleNotFound(new NotFoundException("not found!"),
        req("/x"));
    assertThat(resp.code()).isEqualTo(404);
    assertThat(resp.message()).isEqualTo("not found!");
    assertThat(resp.path()).isEqualTo("/x");
  }

  // ----------------- basic custom exceptions -----------------

  @Test
  @DisplayName("BadRequestException → 400")
  void handleBadRequest() {
    final var resp = handler.handleBadRequest(new BadRequestException("bad!")
        , req(
        "/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).isEqualTo("bad!");
  }

  @Test
  @DisplayName("MethodArgumentNotValidException → 400 (details present)")
  void handleMethodArgumentNotValid() {
    final Object target = new Object();
    final var br = new BeanPropertyBindingResult(target, "target");
    br.addError(new FieldError("target", "name", "must not be blank"));
    final var ex = new MethodArgumentNotValidException(null, br);
    final var resp = handler.handleMethodArgumentNotValid(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.details()).isNotEmpty();
  }

  // ----------------- validation-related -----------------

  @Test
  @DisplayName("ConstraintViolationException → 400 (details present)")
  @SuppressWarnings("unchecked")
  void handleConstraintViolation() {
    final ConstraintViolation<Object> v = mock(ConstraintViolation.class,
        Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(v.getPropertyPath().toString()).thenReturn("page");
    Mockito.when(v.getMessage()).thenReturn("must be greater than 0");
    final var ex = new ConstraintViolationException(Set.of(v));
    final var resp = handler.handleConstraintViolation(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.details()).isNotEmpty();
  }

  @Test
  @DisplayName("BindException → 400 (details present)")
  void handleBindException() {
    final var target = new Object();
    final var br = new BeanPropertyBindingResult(target, "form");
    br.addError(new FieldError("form", "age", "invalid age"));
    final var ex = new BindException(br);
    final var resp = handler.handleBindException(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.details()).isNotEmpty();
  }

  @Test
  @DisplayName("MissingServletRequestParameterException → 400")
  void handleMissingParam() throws Exception {
    final var ex = new MissingServletRequestParameterException("q", "java" +
        ".lang" +
        ".String");
    final var resp = handler.handleMissingParam(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("Missing request parameter: q");
  }

  @Test
  @DisplayName("MethodArgumentTypeMismatch (numeric) → 400")
  void handleTypeMismatch_number() {
    final var ex = new MethodArgumentTypeMismatchException(
        "abc", Integer.class, "num", null, new IllegalArgumentException("not " +
        "int"));
    final var resp = handler.handleTypeMismatch(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("type mismatch");
  }

  // ----------------- type mismatch (incl. enum branch) -----------------

  @Test
  @DisplayName("MethodArgumentTypeMismatch (enum, shows allowed values) → 400")
  void handleTypeMismatch_enum() {
    final var ex = new MethodArgumentTypeMismatchException(
        "INVALID", Level.class, "level", null, new IllegalArgumentException(
        "bad enum"));
    final var resp = handler.handleTypeMismatch(ex, req("/x"));
    assertThat(resp.message()).contains("Allowed values");
  }

  @Test
  @DisplayName("HttpMessageNotReadable → InvalidFormatException (enum) → 400 " +
      "(allowed values)")
  void handleNotReadable_invalidFormat_enum() {
    final var cause = new InvalidFormatException(null, "msg", "INVALID",
        Level.class);
    final var ex =
        new org.springframework.http.converter.HttpMessageNotReadableException("body", cause);
    final var resp = handler.handleNotReadable(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("Allowed values");
  }

  // ----------------- not readable (4-way split) -----------------

  @Test
  @DisplayName("HttpMessageNotReadable → DateTimeParseException → 400 " +
      "(invalid date format)")
  void handleNotReadable_dateTimeParse() {
    final var cause = new DateTimeParseException("bad date", "01-01-2024", 0);
    final var ex =
        new org.springframework.http.converter.HttpMessageNotReadableException("body", cause);
    final var resp = handler.handleNotReadable(ex, req("/x"));
    assertThat(resp.message()).contains("Invalid date format");
  }

  @Test
  @DisplayName("HttpMessageNotReadable → MismatchedInputException (malformed " +
      "structure) → 400")
  void handleNotReadable_mismatchedInput() {
    final MismatchedInputException cause =
        MismatchedInputException.from(null, String.class, "malformed");
    final var ex =
        new org.springframework.http.converter.HttpMessageNotReadableException("body", cause);
    final var resp = handler.handleNotReadable(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("Malformed JSON");
  }

  @Test
  @DisplayName("HttpMessageNotReadable → default fallback → 400")
  void handleNotReadable_default() {
    final var ex =
        new org.springframework.http.converter.HttpMessageNotReadableException(
            "body", new RuntimeException("weird"));
    final var resp = handler.handleNotReadable(ex, req("/x"));
    assertThat(resp.message()).isEqualTo("Malformed JSON request");
  }

  @Test
  @DisplayName("DataIntegrityViolation → unique branch (constraint name " +
      "match) → 409")
  void handleDataIntegrity_unique() {
    final var root = new RuntimeException("violates constraint " +
        "uq_res_client_src_code");
    final var ex = new DataIntegrityViolationException("dup", root);
    final var resp = handler.handleDataIntegrity(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(409);
    assertThat(resp.message()).contains("Duplicate key violation");
  }

  // ----------------- data integrity (3-way split) -----------------

  @Test
  @DisplayName("DataIntegrityViolation → foreign key branch → 409")
  void handleDataIntegrity_fk() {
    final var root = new RuntimeException("FOREIGN KEY constraint fails");
    final var ex = new DataIntegrityViolationException("fk", root);
    final var resp = handler.handleDataIntegrity(ex, req("/x"));
    assertThat(resp.message()).isEqualTo("Foreign key violation");
  }

  @Test
  @DisplayName("DataIntegrityViolation → other branch (pass-through root " +
      "message) → 409")
  void handleDataIntegrity_other() {
    final var root = new RuntimeException("some db error");
    final var ex = new DataIntegrityViolationException("db", root);
    final var resp = handler.handleDataIntegrity(ex, req("/x"));
    assertThat(resp.message()).isEqualTo("some db error");
  }

  @Test
  @DisplayName("MissingPathVariableException → 400")
  void handleMissingPathVariable() throws Exception {
    final Method m = Dummy.class.getDeclaredMethod("foo", String.class);
    final MethodParameter mp = new MethodParameter(m, 0);
    final var ex = new MissingPathVariableException("id", mp);
    final var resp = handler.handleMissingPathVar(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("Missing path variable: id");
  }

  // ----------------- misc -----------------

  @Test
  @DisplayName("MethodArgumentConversionNotSupportedException → 400")
  void handleConversionNotSupported() throws Exception {
    final var ex = new MethodArgumentConversionNotSupportedException(
        "raw", Integer.class, "age", (MethodParameter) null,
        new IllegalArgumentException("no converter"));
    final var resp = handler.handleConversionNotSupported(ex, req("/x"));
    assertThat(resp.code()).isEqualTo(400);
    assertThat(resp.message()).contains("Type conversion not supported");
  }

  @Test
  @DisplayName("Fallback handler (Exception) → 500")
  void handleOthers() {
    final var resp = handler.handleOthers(req("/x"));
    assertThat(resp.code()).isEqualTo(500);
    assertThat(resp.message()).isEqualTo("Internal Server Error");
  }

  enum Level {LOW, MEDIUM, HIGH}

  static class Dummy {
    public void foo(final String id) {
    }
  }
}
