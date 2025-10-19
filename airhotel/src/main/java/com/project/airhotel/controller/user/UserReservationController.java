package com.project.airhotel.controller.user;

import com.project.airhotel.dto.reservation.CreateReservationRequest;
import com.project.airhotel.dto.reservation.PatchReservationRequest;
import com.project.airhotel.dto.reservation.ReservationDetailResponse;
import com.project.airhotel.service.user.UserReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/reservations")
public class UserReservationController {

  private final UserReservationService service;

  public UserReservationController(UserReservationService service) {
    this.service = service;
  }

  private Long currentUserId(Authentication auth) {
    try { return Long.parseLong(auth.getName()); }
    catch (NumberFormatException e) { throw new IllegalStateException("Cannot resolve current user id."); }
  }

//  @GetMapping
//  public List<ReservationSummaryResponse> list(Authentication auth,
//                                               @RequestParam(defaultValue = "0") int page,
//                                               @RequestParam(defaultValue = "20") int size) {
//    return service.listMyReservations(currentUserId(auth));
//  }

  @PostMapping
  public ResponseEntity<ReservationDetailResponse> create(Authentication auth,
                                                          @Valid @RequestBody CreateReservationRequest req) {
    ReservationDetailResponse created = service.createReservation(currentUserId(auth), req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/{id}")
  public ReservationDetailResponse get(Authentication auth, @PathVariable Long id) {
    return service.getMyReservation(currentUserId(auth), id);
  }

  @PatchMapping("/{id}")
  public ReservationDetailResponse patch(Authentication auth,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PatchReservationRequest req) {
    return service.patchMyReservation(currentUserId(auth), id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(Authentication auth, @PathVariable Long id) {
    service.cancelMyReservation(currentUserId(auth), id);
    return ResponseEntity.noContent().build();
  }
}

