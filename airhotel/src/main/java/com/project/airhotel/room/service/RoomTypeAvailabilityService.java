package com.project.airhotel.room.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.dto.RoomTypeAvailabilityResponse;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Computes room-type availability within a hotel for a given stay window.
 */
@Service
@RequiredArgsConstructor
public class RoomTypeAvailabilityService {

  private final RoomTypesRepository roomTypesRepository;
  private final RoomTypeInventoryRepository roomTypeInventoryRepository;
  private final EntityGuards entityGuards;

  /**
   * Returns room types that have availability for the entire stay window.
   *
   * @param hotelId   target hotel id
   * @param checkIn   check-in date inclusive
   * @param checkOut  check-out date exclusive
   * @param numGuests optional guest count filter; when present, filters room
   *                  types whose capacity is less than numGuests
   * @return list of room types with their minimum available count
   */
  public List<RoomTypeAvailabilityResponse> getAvailability(
      final Long hotelId,
      final LocalDate checkIn,
      final LocalDate checkOut,
      final Integer numGuests) {

    validateDates(checkIn, checkOut);
    if (numGuests != null && numGuests <= 0) {
      throw new BadRequestException("numGuests must be positive");
    }

    entityGuards.ensureHotelExists(hotelId);

    final List<RoomTypes> roomTypes =
        roomTypesRepository.findByHotelId(hotelId);
    if (roomTypes.isEmpty()) {
      return List.of();
    }

    final long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    final List<LocalDate> stayDates = new ArrayList<>((int) nights);
    LocalDate cursor = checkIn;
    while (cursor.isBefore(checkOut)) {
      stayDates.add(cursor);
      cursor = cursor.plusDays(1);
    }

    final LocalDate endInclusive = checkOut.minusDays(1);
    final List<RoomTypeInventory> inventories =
        roomTypeInventoryRepository.findByHotelIdAndStayDateBetween(
            hotelId, checkIn, endInclusive);

    final Map<Long, Map<LocalDate, RoomTypeInventory>> invByTypeAndDate =
        inventories.stream().collect(
            Collectors.groupingBy(RoomTypeInventory::getRoomTypeId,
                Collectors.toMap(RoomTypeInventory::getStayDate,
                    inv -> inv, (a, b) -> a))
        );

    final List<RoomTypeAvailabilityResponse> result = new ArrayList<>();
    for (RoomTypes rt : roomTypes) {
      if (numGuests != null && rt.getCapacity() != null
          && rt.getCapacity() < numGuests) {
        continue;
      }
      int minAvailable = rt.getTotalRooms();
      final Map<LocalDate, RoomTypeInventory> datedInv =
          invByTypeAndDate.getOrDefault(rt.getId(), Map.of());
      for (LocalDate d : stayDates) {
        final RoomTypeInventory inv = datedInv.get(d);
        final int available = inv != null && inv.getAvailable() != null
            ? inv.getAvailable()
            : rt.getTotalRooms();
        minAvailable = Math.min(minAvailable, available);
        if (minAvailable <= 0) {
          break;
        }
      }
      if (minAvailable > 0) {
        result.add(RoomTypeAvailabilityResponse.builder()
            .roomTypeId(rt.getId())
            .code(rt.getCode())
            .name(rt.getName())
            .bedType(rt.getBedType())
            .capacity(rt.getCapacity())
            .totalRooms(rt.getTotalRooms())
            .available(minAvailable)
            .baseRate(rt.getBaseRate())
            .build());
      }
    }
    return result;
  }

  private void validateDates(final LocalDate checkIn, final LocalDate checkOut) {
    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("checkIn and checkOut are required");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new BadRequestException("checkOut must be after checkIn");
    }
  }
}
