package com.project.airhotel.hotel.service;

import com.project.airhotel.hotel.domain.Hotels;
import com.project.airhotel.hotel.repository.HotelsRepository;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import com.project.airhotel.room.repository.RoomsRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer handling hotel-related operations such as retrieving hotel information, counting
 * rooms, and fetching room type availability.
 */
@Service
public final class HotelService {

  /**
   * Repository for hotel entities.
   */
  private final HotelsRepository hotelsRepository;

  /**
   * Repository for room entities.
   */
  private final RoomsRepository roomsRepository;

  /**
   * Repository for room type entities.
   */
  private final RoomTypesRepository roomTypesRepository;

  /**
   * Repository for room type inventory entities.
   */
  private final RoomTypeInventoryRepository roomTypeInventoryRepository;

  /**
   * Constructs the service with all required repositories.
   *
   * @param hotelsRepo      repository for hotel entities
   * @param roomsRepo       repository for room entities
   * @param roomTypesRepo   repository for room type entities
   * @param roomTypeInvRepo repository for room type inventory
   */
  public HotelService(
      final HotelsRepository hotelsRepo,
      final RoomsRepository roomsRepo,
      final RoomTypesRepository roomTypesRepo,
      final RoomTypeInventoryRepository roomTypeInvRepo) {

    this.hotelsRepository = hotelsRepo;
    this.roomsRepository = roomsRepo;
    this.roomTypesRepository = roomTypesRepo;
    this.roomTypeInventoryRepository = roomTypeInvRepo;
  }

  /**
   * Retrieves all hotels stored in the repository.
   *
   * @return a list of all {@link Hotels} entities
   */
  public List<Hotels> getAllHotels() {
    return hotelsRepository.findAll();
  }

  /**
   * Retrieves a specific hotel by its ID.
   *
   * @param id the unique identifier of the hotel
   * @return the {@link Hotels} entity if found
   * @throws ResponseStatusException if the hotel is not found
   */
  public Hotels getById(final Long id) {
    return hotelsRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Hotel not found"));
  }

  /**
   * Counts the total number of rooms belonging to a specific hotel.
   *
   * @param hotelId the unique identifier of the hotel
   * @return the total number of rooms for the given hotel
   */
  public long countRooms(final Long hotelId) {
    return roomsRepository.countByHotelId(hotelId);
  }

  /**
   * Retrieves all room types and their available counts for a specific hotel.
   *
   * @param hotelId the unique identifier of the hotel
   * @return a list of maps, each containing room type info and availability
   */
  public List<Map<String, Object>> getRoomTypeAvailability(
      final Long hotelId) {

    if (!hotelsRepository.existsById(hotelId)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Hotel not found");
    }

    final LocalDate today = LocalDate.now();

    final List<RoomTypes> roomTypes =
        roomTypesRepository.findByHotelId(hotelId);

    final List<RoomTypeInventory> inventories =
        roomTypeInventoryRepository
            .findByHotelIdAndStayDate(hotelId, today);

    final Map<Long, RoomTypeInventory> inventoryMap =
        inventories.stream().collect(Collectors.toMap(
            RoomTypeInventory::getRoomTypeId,
            inv -> inv));

    final List<Map<String, Object>> result = new ArrayList<>();

    for (RoomTypes rt : roomTypes) {
      final RoomTypeInventory inv = inventoryMap.get(rt.getId());

      result.add(Map.of(
          "roomTypeId", rt.getId(),
          "code", rt.getCode(),
          "name", rt.getName(),
          "bedType", rt.getBedType(),
          "capacity", rt.getCapacity(),
          "totalRooms", rt.getTotalRooms(),
          "available", inv != null ? inv.getAvailable() : 0
      ));
    }

    return result;
  }

  /**
   * Performs a fuzzy search for hotels based on city name prefix.
   *
   * <p>This method returns hotels whose city names <b>start with</b> the
   * provided keyword (case-insensitive). For example, keyword "new" will match "New York", "New
   * Haven", "New Orleans", but will not match "Renew Hotel".
   *
   * @param cityKeyword a partial city name prefix, e.g. "new"
   * @return a list of hotels whose city starts with the keyword
   * @throws ResponseStatusException if the search keyword is empty
   */
  public List<Hotels> searchHotelsByCityFuzzy(final String cityKeyword) {
    if (cityKeyword == null || cityKeyword.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "City keyword must not be empty");
    }
    return hotelsRepository.searchCityByPrefix(cityKeyword.trim());
  }

  /**
   * Performs a fuzzy city search and filters hotels by availability in the given stay date range.
   *
   * <p>The date range is interpreted as [startDate, endDate), where
   * startDate is inclusive and endDate is exclusive.</p>
   *
   * @param keyword   fuzzy city keyword such as "new"
   * @param startDate inclusive check-in date
   * @param endDate   exclusive check-out date
   * @return hotels that match the city keyword and have at least one available room type on every
   * date in the range
   * @throws ResponseStatusException if keyword is blank or dates are invalid
   */
  public List<Hotels> searchAvailableHotelsByCityAndDates(
      final String keyword,
      final LocalDate startDate,
      final LocalDate endDate) {

    if (keyword == null || keyword.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "City keyword must not be empty"
      );
    }

    if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid start/end date"
      );
    }

    List<Hotels> candidates =
        hotelsRepository.searchCityByPrefix(keyword.trim());

    return candidates.stream()
        .filter(hotel ->
            isHotelAvailable(
                hotel.getId()))
        .collect(Collectors.toList());
  }

  /**
   * Checks whether a given hotel has at least one available room for every date in the given range
   * [startDate, endDate).
   *
   * @param hotelId hotel id to check
   * @return {@code true} if the hotel is available on every day, {@code false} otherwise
   */
  private boolean isHotelAvailable(
      final Long hotelId) {
    final List<RoomTypes> roomTypesFound = roomTypesRepository.findByHotelId(hotelId);
    return !roomTypesFound.isEmpty();
  }

}
