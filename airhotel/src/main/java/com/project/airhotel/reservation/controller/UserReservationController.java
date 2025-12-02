package com.project.airhotel.reservation.controller;

import com.project.airhotel.config.SpringConfig;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationDetailResponse;
import com.project.airhotel.reservation.dto.ReservationSummaryResponse;
import com.project.airhotel.reservation.service.UserReservationService;
import com.project.airhotel.user.service.AuthUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User-facing REST controller for reservation operations. Exposes endpoints to
 * list, create, view, update, and cancel reservations owned by the
 * authenticated user. User identification is resolved from the session first
 * and, if absent, from the OAuth2 principal which is then persisted into the
 * session.
 * Base path: /reservations
 */
@RestController
@Validated
@RequestMapping("/reservations")
public class UserReservationController {

  /**
   * Application service implementing user reservation use cases.
   */
  private final UserReservationService service;

  /**
   * Service that resolves or creates an internal user from an authenticated
   * principal.
   */
  private final AuthUserService authUserService;

  /**
   * Constructs the controller with required dependencies.
   *
   * @param serv         application service for user reservations
   * @param authUserServ service to find or create users by email for session
   *                     binding
   */
  public UserReservationController(final UserReservationService serv,
                                   final AuthUserService authUserServ) {
    this.service = serv;
    this.authUserService = authUserServ;
  }

  /**
   * Resolves the current user's id from the HTTP session if present; otherwise,
   * extracts email and name from the OAuth2 principal, finds or creates the
   * user, stores the resolved id in the session, and returns it.
   * Resolution order:
   * 1) Read from session attribute SpringConfig.SESSION_USER_ID if available
   * 2) Fallback to OAuth2 principal (email and name) and persist id back to
   * session
   *
   * @param auth    Spring Security authentication holding the OAuth2 principal
   * @param request HTTP servlet request used to access and write the session
   * @return the resolved internal user id
   * @throws IllegalStateException if the principal has no email when fallback
   *                               is required
   */
  private Long currentUserId(final Authentication auth,
                             final HttpServletRequest request) {
    // 1) Take user id from the session first.
    final Object v = request.getSession(false) == null ? null
        : request.getSession(false).getAttribute(
        SpringConfig.SESSION_USER_ID);
    if (v instanceof final Long id) {
      return id;
    }
    // 2) backup plan: fetch email from OAuth2User, then lookup/create user
    // and write back to Session
    final OAuth2User p = (OAuth2User) auth.getPrincipal();
    final String email = p.getAttribute("email");
    final String name = p.getAttribute("name");
    final Long userId = authUserService.findOrCreateByEmail(email, name);
    request.getSession(true).setAttribute(SpringConfig.SESSION_USER_ID,
        userId);
    return userId;
  }

  /**
   * Lists reservations owned by the current user.
   * GET /reservations
   *
   * @param auth    Spring Security authentication
   * @param request HTTP request used for session access
   * @return list of reservation summaries
   */
  @GetMapping
  public List<ReservationSummaryResponse> list(
      final Authentication auth,
      final HttpServletRequest request) {
    return service.listMyReservations(currentUserId(auth, request));
  }

  /**
   * Creates a reservation for the current user.
   * POST /reservations
   *
   * @param auth    Spring Security authentication
   * @param request HTTP request used for session access
   * @param req     creation payload
   * @return 201 Created with reservation detail in the response body
   */
  @PostMapping
  public ResponseEntity<ReservationDetailResponse> create(
      final Authentication auth,
      final HttpServletRequest request,
      @Valid @RequestBody final CreateReservationRequest req) {
    final Long userId = currentUserId(auth, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createReservation(userId, req));
  }

  /**
   * Retrieves a single reservation owned by the current user.
   * GET /reservations/{id}
   *
   * @param auth    Spring Security authentication
   * @param request HTTP request used for session access
   * @param id      reservation identifier
   * @return detailed reservation response
   */
  @GetMapping("/{id}")
  public ReservationDetailResponse get(final Authentication auth,
                                       final HttpServletRequest request,
                                       @PathVariable final Long id) {
    return service.getMyReservation(currentUserId(auth, request), id);
  }

  /**
   * Partially updates a reservation owned by the current user.
   * PATCH /reservations/{id}
   *
   * @param auth    Spring Security authentication
   * @param request HTTP request used for session access
   * @param id      reservation identifier
   * @param req     patch payload
   * @return detailed reservation response after update
   */
  @PatchMapping("/{id}")
  public ReservationDetailResponse patch(
      final Authentication auth,
      final HttpServletRequest request,
      @PathVariable final Long id,
      @Valid @RequestBody final PatchReservationRequest req) {
    return service.patchMyReservation(currentUserId(auth, request), id, req);
  }

  /**
   * Cancels a reservation owned by the current user. Returns 204 No Content on
   * success.
   * DELETE /reservations/{id}
   *
   * @param auth    Spring Security authentication
   * @param request HTTP request used for session access
   * @param id      reservation identifier
   * @return empty 204 response on success
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(final Authentication auth,
                                     final HttpServletRequest request,
                                     @PathVariable final Long id) {
    service.cancelMyReservation(currentUserId(auth, request), id);
    return ResponseEntity.noContent().build();
  }
}
