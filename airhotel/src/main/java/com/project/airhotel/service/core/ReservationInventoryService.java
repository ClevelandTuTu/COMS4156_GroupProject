package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.RoomTypeInventory;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.repository.RoomTypeInventoryRepository;
import com.project.airhotel.repository.RoomTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Ziyang Su
 * @version 1.0.0
 * todo: add consistency between room_types table and room_type_inventory table
 */
@Service
@RequiredArgsConstructor
public class ReservationInventoryService {
  /** Repository for day-level inventory rows. */
  private final RoomTypeInventoryRepository invRepo;
  /** Repository for room type master data. */
  private final RoomTypesRepository roomTypesRepo;

  /**
   * Pre-occupy one unit of inventory for each date in [checkIn, checkOut). This
   * method validates availability first to avoid partial success. If any date
   * has no availability, the operation fails with an exception.
   *
   * @param hotelId    the hotel identifier
   * @param roomTypeId the room type identifier
   * @param checkIn    inclusive start date
   * @param checkOut   exclusive end date
   * @throws BadRequestException if parameters are invalid or no availability
   *                             exists for any date
   */
  public void reserveRangeOrThrow(final Long hotelId, final Long roomTypeId,
                                  final LocalDate checkIn,
                                  final LocalDate checkOut) {
    ensureParams(hotelId, roomTypeId, checkIn, checkOut);

    final List<LocalDate> days = datesBetween(checkIn, checkOut);
    // Check before batch save to avoid half successes, half failure
    final List<RoomTypeInventory> toSave = new ArrayList<>(days.size());
    for (final LocalDate d : days) {
      final RoomTypeInventory inv = getOrInitInventoryRow(hotelId, roomTypeId,
          d);
      final int available =
          inv.getTotal() - inv.getReserved() - inv.getBlocked();
      if (available <= 0) {
        throw new BadRequestException("No availability on " + d + " for this "
            + "room type.");
      }
      inv.setReserved(inv.getReserved() + 1);
      inv.setAvailable(inv.getTotal() - inv.getReserved() - inv.getBlocked());
      toSave.add(inv);
    }
    invRepo.saveAll(toSave);
  }

  /**
   * Release one unit of inventory for each date in [checkIn, checkOut). If the
   * inventory row does not exist for a date, nothing is changed for that date.
   * Reserved cannot drop below zero.
   *
   * @param hotelId    the hotel identifier
   * @param roomTypeId the room type identifier
   * @param checkIn    inclusive start date
   * @param checkOut   exclusive end date
   * @throws BadRequestException if parameters are invalid
   */
  public void releaseRange(final Long hotelId, final Long roomTypeId,
                           final LocalDate checkIn,
                           final LocalDate checkOut) {
    ensureParams(hotelId, roomTypeId, checkIn, checkOut);

    for (final LocalDate d : datesBetween(checkIn, checkOut)) {
      invRepo.findForUpdate(hotelId, roomTypeId, d).ifPresent(inv -> {
        final int reserved = Math.max(0, inv.getReserved() - 1);
        inv.setReserved(reserved);
        inv.setAvailable(inv.getTotal() - reserved - inv.getBlocked());
        invRepo.save(inv);
      });
    }
  }

  /**
   * Validate required parameters and basic date range correctness.
   *
   * @param hotelId    hotel id, must be non-null
   * @param roomTypeId room type id, must be non-null
   * @param checkIn    inclusive start date, must be non-null
   * @param checkOut   exclusive end date, must be after checkIn
   * @throws BadRequestException if any validation fails
   */
  private void ensureParams(final Long hotelId, final Long roomTypeId,
                            final LocalDate checkIn,
                            final LocalDate checkOut) {
    if (hotelId == null || roomTypeId == null || checkIn == null
        || checkOut == null) {
      throw new BadRequestException("Missing inventory parameters.");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new BadRequestException("Invalid stay date range.");
    }
  }

  /**
   * Load an inventory row for a given day with a lock, or create it if missing.
   * The created row derives total and available from the room type definition.
   *
   * @param hotelId    hotel id
   * @param roomTypeId room type id
   * @param stayDate   target date
   * @return existing or newly created inventory row
   * @throws BadRequestException if the room type does not exist or does not
   *                             belong to the hotel
   */
  private RoomTypeInventory getOrInitInventoryRow(final Long hotelId,
                                                  final Long roomTypeId,
                                                  final LocalDate stayDate) {
    return invRepo.findForUpdate(hotelId, roomTypeId, stayDate)
        .orElseGet(() -> {
          final RoomTypes rt = roomTypesRepo.findById(roomTypeId)
              .orElseThrow(() -> new BadRequestException("Room type not "
                  + "found: " + roomTypeId));
          if (!Objects.equals(rt.getHotelId(), hotelId)) {
            throw new BadRequestException("Room type does not belong to "
                + "hotel: " + hotelId);
          }
          final RoomTypeInventory created = RoomTypeInventory.builder()
              .hotelId(hotelId)
              .roomTypeId(roomTypeId)
              .stayDate(stayDate)
              .total(rt.getTotalRooms())
              .reserved(0)
              .blocked(0)
              .available(rt.getTotalRooms())
              .build();
          return invRepo.save(created);
        });
  }


  /**
   * Create a list of consecutive dates from startInclusive up to but excluding
   * endExclusive.
   *
   * @param startInclusive start date inclusive
   * @param endExclusive   end date exclusive
   * @return ordered list of dates, or an empty list if the range is invalid
   */
  private List<LocalDate> datesBetween(final LocalDate startInclusive,
                                       final LocalDate endExclusive) {
    final long days = endExclusive.toEpochDay() - startInclusive.toEpochDay();
    if (days <= 0) {
      return List.of();
    }
    final List<LocalDate> res = new ArrayList<>((int) days);
    LocalDate cur = startInclusive;
    for (int i = 0;
         i < days;
         i++) {
      res.add(cur);
      cur = cur.plusDays(1);
    }
    return res;
  }
}
