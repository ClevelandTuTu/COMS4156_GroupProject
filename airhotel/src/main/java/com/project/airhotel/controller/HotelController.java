package com.project.airhotel.controller;

import java.time.LocalDate;
import com.project.airhotel.service.publicApi.HotelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller exposing read-only hotel APIs.
 * <p>Base path: {@code /hotels}</p>
 */
@RestController
@RequestMapping("/hotels")
public final class HotelController {

  /**
   * Business service used for hotel queries.
   */
  private final HotelService hotelService;

  /**
   * Constructs the controller with its required dependency.
   *
   * @param hotelServiceParam injected {@link HotelService} instance
   */
  public HotelController(final HotelService hotelServiceParam) {
    this.hotelService = hotelServiceParam;
  }

  /**
   * Returns all hotels.
   *
   * @return list of hotels
   */
  @GetMapping
  public List<?> getAllHotels() {
    return hotelService.getAllHotels();
  }

  /**
   * Returns a single hotel's basic information with a composed address.
   *
   * @param id hotel id
   * @return map containing id, name and full address
   */
  @GetMapping("/{id}")
  public Map<String, Object> getHotel(@PathVariable final Long id) {
    var h = hotelService.getById(id);

    String countryAndPostal = Stream
        .of(h.getCountry(), h.getPostalCode())
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(" "));

    String fullAddress = Stream
        .of(
            h.getAddressLine1(),
            h.getAddressLine2(),
            h.getCity(),
            h.getState(),
            countryAndPostal
        )
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(", "));

    return Map.of(
        "id", h.getId(),
        "name", h.getName(),
        "address", fullAddress
    );
  }

  /**
   * Returns all room types of the given hotel (no daily price).
   *
   * @param id hotel id
   * @return list of room-type maps
   */
  @GetMapping("/{id}/room-types")
  public List<Map<String, Object>> getHotelRoomTypes(
      @PathVariable final Long id) {
    return hotelService.getRoomTypeAvailability(id);
  }


  /**
   * Performs a fuzzy search for hotels by city name. This endpoint returns
   * hotels whose city names contain the provided keyword, ignoring case.
   *
   * <p>Example: {@code GET /hotels/search?city=new}</p>
   *
   * @param city the partial or full city name keyword
   * @return list of hotels whose city contains the keyword
   */
  @GetMapping("/search")
  public List<?> searchHotelsByCity(@RequestParam("city") final String city) {
    return hotelService.searchHotelsByCityFuzzy(city);
  }

  /**
   * Searches for hotels by fuzzy city name and filters them by availability
   * within the given stay date range.
   *
   * Example:
   *   GET /hotels/search/available?city=new
   *   &startDate=2025-01-10&endDate=2025-01-11,
   *   2025-01-13, 2025-01-14.
   *
   * @param city      partial or full city name keyword
   * @param startDate inclusive check-in date (ISO yyyy-MM-dd)
   * @param endDate   exclusive check-out date (ISO yyyy-MM-dd)
   * @return list of hotels that match the city keyword and are available
   *         for each date in [startDate, endDate)
   */
  @GetMapping("/search/available")
  public List<?> searchAvailableHotels(
      @RequestParam("city") final String city,
      @RequestParam("startDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      final LocalDate startDate,
      @RequestParam("endDate")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      final LocalDate endDate) {

    return hotelService.searchAvailableHotelsByCityAndDates(
        city, startDate, endDate);
  }

}
