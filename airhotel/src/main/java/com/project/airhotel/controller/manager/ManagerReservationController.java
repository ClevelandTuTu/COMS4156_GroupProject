package com.project.airhotel.controller.manager;

import com.project.airhotel.dto.reservation.ApplyUpgradeRequest;
import com.project.airhotel.dto.reservation.ReservationUpdateRequest;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.service.manager.ManagerReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
 * Manager-facing REST controller for reservation operations. Exposes endpoints
 * to list, view, update, upgrade, check in, check out, and cancel reservations
 * for a specific hotel.
 * <p>
 * Base path: /manager/hotels/{hotelId}/reservations
 * <p>
 * All business logic is delegated to ManagerReservationService.
 */
@Validated
@RestController
@RequestMapping("/manager/hotels/{hotelId}/reservations")
public class ManagerReservationController {

  /**
   * Application service that implements manager-side reservation use cases.
   */
  private final ManagerReservationService reservationService;

  /**
   * Constructs the controller with its required service dependency.
   *
   * @param reservationServ manager reservation application service
   */
  public ManagerReservationController(
      final ManagerReservationService reservationServ) {
    this.reservationService = reservationServ;
  }


  /**
   * Lists reservations for a hotel with optional filters. If both status and
   * date range are provided, both filters apply. If only date range is
   * provided, results are filtered by stay range. If only status is provided,
   * results are filtered by status. If no filters are provided, returns all
   * reservations for the hotel.
   * <p>
   * GET /manager/hotels/{hotelId}/reservations
   *
   * @param hotelId hotel identifier
   * @param status  optional reservation status filter
   * @param start   optional start of the stay range in ISO date (inclusive)
   * @param end     optional end of the stay range in ISO date (exclusive)
   * @return list of reservations matching the filters
   */
  @GetMapping
  public List<Reservations> list(
      @PathVariable final Long hotelId,
      @RequestParam(required = false) final ReservationStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso =
          DateTimeFormat.ISO.DATE) final LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso =
          DateTimeFormat.ISO.DATE) final LocalDate end) {
    return reservationService.listReservations(hotelId, status, start, end);
  }


  /**
   * Retrieves a single reservation by id under the given hotel.
   * <p>
   * GET /manager/hotels/{hotelId}/reservations/{reservationId}
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @return reservation entity
   */
  @GetMapping("/{reservationId}")
  public Reservations get(@PathVariable final Long hotelId,
                          @PathVariable final Long reservationId) {
    return reservationService.getReservation(hotelId, reservationId);
  }


  /**
   * Partially updates a reservation. Supports changing room type, room id, stay
   * dates, and scalar fields, and may also change status via the status
   * machine.
   * <p>
   * PATCH /manager/hotels/{hotelId}/reservations/{reservationId}
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @param req           update payload
   * @return updated reservation
   */
  @PatchMapping("/{reservationId}")
  public Reservations patch(
      @PathVariable final Long hotelId,
      @PathVariable final Long reservationId,
      @Valid @RequestBody final ReservationUpdateRequest req) {
    return reservationService.patchReservation(hotelId, reservationId, req);
  }


  /**
   * Applies a room-type upgrade to a reservation when allowed by its upgrade
   * status.
   * <p>
   * PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/apply-upgrade
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @param req           request containing the new roomTypeId
   * @return reservation after upgrade is applied
   */
  @PatchMapping("/{reservationId}/apply-upgrade")
  public Reservations applyUpgrade(
      @PathVariable final Long hotelId,
      @PathVariable final Long reservationId,
      @Valid @RequestBody final ApplyUpgradeRequest req) {
    return reservationService.applyUpgrade(hotelId, reservationId, req);
  }


  /**
   * Checks in a reservation if eligible.
   * <p>
   * PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/check-in
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @return reservation after check-in transition
   */
  @PatchMapping("/{reservationId}/check-in")
  public Reservations checkIn(@PathVariable final Long hotelId,
                              @PathVariable final Long reservationId) {
    return reservationService.checkIn(hotelId, reservationId);
  }


  /**
   * Checks out a reservation if eligible.
   * <p>
   * PATCH /manager/hotels/{hotelId}/reservations/{reservationId}/check-out
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @return reservation after check-out transition
   */
  @PatchMapping("/{reservationId}/check-out")
  public Reservations checkOut(@PathVariable final Long hotelId,
                               @PathVariable final Long reservationId) {
    return reservationService.checkOut(hotelId, reservationId);
  }

  /**
   * Cancels a reservation. Returns 204 No Content on success.
   * <p>
   * DELETE /manager/hotels/{hotelId}/reservations/{reservationId}
   *
   * @param hotelId       hotel identifier
   * @param reservationId reservation identifier
   * @param reason        optional textual reason for the cancelation
   * @return empty 204 response on success
   */
  @DeleteMapping("/{reservationId}")
  public ResponseEntity<Void> cancel(
      @PathVariable final Long hotelId,
      @PathVariable final Long reservationId,
      @RequestParam(required = false) final String reason) {
    reservationService.cancel(hotelId, reservationId, reason);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
