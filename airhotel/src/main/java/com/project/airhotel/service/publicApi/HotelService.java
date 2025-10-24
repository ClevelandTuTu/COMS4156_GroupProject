package com.project.airhotel.service.publicApi;

import com.project.airhotel.model.Hotels;
import com.project.airhotel.model.RoomTypeInventory;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.repository.HotelsRepository;
import com.project.airhotel.repository.RoomTypeInventoryRepository;
import com.project.airhotel.repository.RoomTypesRepository;
import com.project.airhotel.repository.RoomsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer handling hotel-related operations such as retrieving
 * hotel information, counting rooms, and fetching room type availability.
 */
@Service
public final class HotelService {

  /** Repository for hotel entities. */
  private final HotelsRepository hotelsRepository;

  /** Repository for room entities. */
  private final RoomsRepository roomsRepository;

  /** Repository for room type entities. */
  private final RoomTypesRepository roomTypesRepository;

  /** Repository for room type inventory entities. */
  private final RoomTypeInventoryRepository roomTypeInventoryRepository;

  /**
   * Constructs the service with all required repositories.
   *
   * @param hotelsRepo           repository for hotel entities
   * @param roomsRepo            repository for room entities
   * @param roomTypesRepo        repository for room type entities
   * @param roomTypeInvRepo      repository for room type inventory
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
}
