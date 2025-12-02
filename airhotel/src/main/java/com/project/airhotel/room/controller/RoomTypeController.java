package com.project.airhotel.room.controller;

import com.project.airhotel.room.dto.RoomTypeAvailabilityResponse;
import com.project.airhotel.room.service.RoomTypeAvailabilityService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing room-type availability for guests.
 */
@RestController
@RequestMapping("/hotels/{hotelId}/room-types")
public class RoomTypeController {

  private final RoomTypeAvailabilityService availabilityService;

  public RoomTypeController(
      final RoomTypeAvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }

  /**
   * Returns room types that have availability for the entire stay window.
   *
   * @param hotelId   hotel id
   * @param checkIn   check-in date (inclusive)
   * @param checkOut  check-out date (exclusive)
   * @param numGuests optional guest count filter
   * @return list of room types with availability
   */
  @GetMapping("/availability")
  public List<RoomTypeAvailabilityResponse> getAvailability(
      @PathVariable final Long hotelId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      final LocalDate checkIn,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      final LocalDate checkOut,
      @RequestParam(required = false) final Integer numGuests) {
    return availabilityService.getAvailability(
        hotelId, checkIn, checkOut, numGuests);
  }
}
