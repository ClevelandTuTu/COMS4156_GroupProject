package com.project.airhotel.controller;

import com.project.airhotel.dto.reservations.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservations.ReservationUpdateRequest;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.service.ManagerReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@RestController
@RequestMapping("/manager/hotels/{hotelId}/reservations")
public class ManagerReservationController {


  private final ManagerReservationService reservationService;


  public ManagerReservationController(ManagerReservationService reservationService) {
    this.reservationService = reservationService;
  }


  /**
   * GET /manager/hotels/{hotelId}/reservations?status=CONFIRMED&start=2025-10-01&end=2025-10-31
   */
  @GetMapping
  public List<Reservations> list(
      @PathVariable Long hotelId,
      @RequestParam(required = false) ReservationStatus status,
      @RequestParam(required = false) LocalDate start,
      @RequestParam(required = false) LocalDate end) {
    return reservationService.listReservations(hotelId, status, start, end);
  }


  /** GET /manager/hotels/{hotelId}/reservations/{reservationId} */
  @GetMapping("/{reservationId}")
  public Reservations get(@PathVariable Long hotelId,
                          @PathVariable Long reservationId) {
    return reservationService.getReservation(hotelId, reservationId);
  }


  /** PATCH /manager/hotels/{hotelId}/reservations/{reservationId} */
  @PatchMapping("/{reservationId}")
  public Reservations patch(@PathVariable Long hotelId,
                            @PathVariable Long reservationId,
                            @Valid @RequestBody ReservationUpdateRequest req) {
    return reservationService.patchReservation(hotelId, reservationId, req);
  }


  /** PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/apply-upgrade */
  @PatchMapping("/{reservationId}/apply-upgrade")
  public Reservations applyUpgrade(@PathVariable Long hotelId,
                                   @PathVariable Long reservationId,
                                   @Valid @RequestBody ApplyUpgradeRequest req) {
    return reservationService.applyUpgrade(hotelId, reservationId, req);
  }


  /** PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/check-in */
  @PatchMapping("/{reservationId}/check-in")
  public Reservations checkIn(@PathVariable Long hotelId,
                              @PathVariable Long reservationId) {
    return reservationService.checkIn(hotelId, reservationId);
  }


  /** PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/check-out */
  @PatchMapping("/{reservationId}/check-out")
  public Reservations checkOut(@PathVariable Long hotelId,
                               @PathVariable Long reservationId) {
    return reservationService.checkOut(hotelId, reservationId);
  }

  /** DELETE /manager/hotels/{hotelId}/reservations/{reservationId} */
  @DeleteMapping("/{reservationId}")
  public ResponseEntity<Void> cancel(@PathVariable Long hotelId,
                                     @PathVariable Long reservationId,
                                     @RequestParam(required = false) String reason) {
    reservationService.cancel(hotelId, reservationId, reason);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
