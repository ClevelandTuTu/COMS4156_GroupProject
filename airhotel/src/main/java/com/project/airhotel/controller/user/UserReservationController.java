package com.project.airhotel.controller.user;

import com.project.airhotel.SpringConfig;
import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
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
    // 1) 先从 Session 里拿
    Object v = request.getSession(false) == null ? null
        : request.getSession(false).getAttribute(SpringConfig.SESSION_USER_ID);
    if (v instanceof Long id) {
      return id;
    }
    // 2) 兜底：从 OAuth2User 取 email，再查/建用户并写回 Session
    OAuth2User p = (OAuth2User) auth.getPrincipal();
    String email = p.getAttribute("email");
    String name  = p.getAttribute("name");
    Long userId = authUserService.findOrCreateByEmail(email, name);
    request.getSession(true).setAttribute(SpringConfig.SESSION_USER_ID, userId);
    return userId;
  }

//  @GetMapping
//  public List<ReservationSummaryResponse> list(Authentication auth,
//                                               HttpServletRequest request,
//                                               @RequestParam(defaultValue = "0") int page,
//                                               @RequestParam(defaultValue = "20") int size) {
//    return service.listMyReservations(currentUserId(auth, request));
//  }

  @PostMapping
  public ResponseEntity<ReservationDetailResponse> create(Authentication auth,
                                                          HttpServletRequest request,
                                                          @Valid @RequestBody CreateReservationRequest req) {
    Long userId = currentUserId(auth, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createReservation(userId, req));
  }

  @GetMapping("/{id}")
  public ReservationDetailResponse get(Authentication auth,
                                       HttpServletRequest request,
                                       @PathVariable Long id) {
    return service.getMyReservation(currentUserId(auth, request), id);
  }

  @PatchMapping("/{id}")
  public ReservationDetailResponse patch(Authentication auth,
                                         HttpServletRequest request,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PatchReservationRequest req) {
    return service.patchMyReservation(currentUserId(auth, request), id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(Authentication auth,
                                     HttpServletRequest request,
                                     @PathVariable Long id) {
    service.cancelMyReservation(currentUserId(auth, request), id);
    return ResponseEntity.noContent().build();
  }
}
