package com.project.airhotel.controller.user;

import com.project.airhotel.SpringConfig;
import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.dto.reservation.ReservationSummaryResponse;
import com.project.airhotel.service.auth.AuthUserService;
import com.project.airhotel.service.user.UserReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/reservations")
public class UserReservationController {

  private final UserReservationService service;
  private final AuthUserService authUserService;

  public UserReservationController(UserReservationService service,
                                   AuthUserService authUserService) {
    this.service = service;
    this.authUserService = authUserService;
  }

  private Long currentUserId(Authentication auth, HttpServletRequest request) {
    // 1) Take user id from the session first.
    Object v = request.getSession(false) == null ? null
        : request.getSession(false).getAttribute(SpringConfig.SESSION_USER_ID);
    if (v instanceof Long id) {
      return id;
    }
    // 2) backup plan: fetch email from OAuth2User, then lookup/create user and write back to Session
    OAuth2User p = (OAuth2User) auth.getPrincipal();
    String email = p.getAttribute("email");
    String name  = p.getAttribute("name");
    Long userId = authUserService.findOrCreateByEmail(email, name);
    request.getSession(true).setAttribute(SpringConfig.SESSION_USER_ID, userId);
    return userId;
  }

  /** GET /reservations */
  @GetMapping
  public List<ReservationSummaryResponse> list(Authentication auth,
                                               HttpServletRequest request) {
    return service.listMyReservations(currentUserId(auth, request));
  }

  /** POST /reservations */
  @PostMapping
  public ResponseEntity<ReservationDetailResponse> create(Authentication auth,
                                                          HttpServletRequest request,
                                                          @Valid @RequestBody CreateReservationRequest req) {
    Long userId = currentUserId(auth, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createReservation(userId, req));
  }

  /** GET /reservations/{id} */
  @GetMapping("/{id}")
  public ReservationDetailResponse get(Authentication auth,
                                       HttpServletRequest request,
                                       @PathVariable Long id) {
    return service.getMyReservation(currentUserId(auth, request), id);
  }

  /** PATCH /reservations/{id} */
  @PatchMapping("/{id}")
  public ReservationDetailResponse patch(Authentication auth,
                                         HttpServletRequest request,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PatchReservationRequest req) {
    return service.patchMyReservation(currentUserId(auth, request), id, req);
  }

  /** DELETE /reservations/{id} */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(Authentication auth,
                                     HttpServletRequest request,
                                     @PathVariable Long id) {
    service.cancelMyReservation(currentUserId(auth, request), id);
    return ResponseEntity.noContent().build();
  }
}
