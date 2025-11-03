package com.project.airhotel.reservation.service;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Ziyang Su
 * @version 1.0.0
 * todo: add consistency between room_types table and room_type_inventory table
 * todo: add room_id and new room_type corresponding
 */
@Service
@RequiredArgsConstructor
public class ReservationInventoryService {
  /**
   * Repository for day-level inventory rows.
   */
  private final RoomTypeInventoryRepository invRepo;
  /**
   * Repository for room type master data.
   */
  private final RoomTypesRepository roomTypesRepo;

  /**
   * Create a list of consecutive dates from startInclusive up to but excluding
   * endExclusive.
   *
   * @param startInclusive start date inclusive
   * @param endExclusive   end date exclusive
   * @return ordered list of dates, or an empty list if the range is invalid
   */
  private static Set<LocalDate> toDaySet(final LocalDate startInclusive,
                                         final LocalDate endExclusive) {
    final long days = endExclusive.toEpochDay() - startInclusive.toEpochDay();
    if (days <= 0) {
      return Collections.emptySet();
    }
    final Set<LocalDate> set = new HashSet<>((int) days * 2);
    LocalDate cur = startInclusive;
    for (int i = 0;
         i < days;
         i++) {
      set.add(cur);
      cur = cur.plusDays(1);
    }
    return set;
  }

  /**
   * Unified inventory adjustment entry Allow either side of old/new to be empty
   * (indicating an empty set) Two stages: First, "pre-check net increase"
   * availability, and then "apply net decrease/net increase"
   */
  public void applyRangeChangeOrThrow(final Long hotelId,
                                      @Nullable final Long oldTypeId,
                                      @Nullable final LocalDate oldIn,
                                      @Nullable final LocalDate oldOut,
                                      @Nullable final Long newTypeId,
                                      @Nullable final LocalDate newIn,
                                      @Nullable final LocalDate newOut) {

    final Range oldRange = normalizeRange("old", hotelId, oldTypeId, oldIn,
        oldOut);
    final Range newRange = normalizeRange("new", hotelId, newTypeId, newIn,
        newOut);

    // Both sides are empty and there is no operation
    if (oldRange.isEmpty() && newRange.isEmpty()) {
      return;
    }

    // 1) Pre-check: Only check availability for "net new additions"
    // Net increase = newDays - (oldDays and the same room type)
    if (!newRange.isEmpty()) {
      precheckNetAddsOrThrow(hotelId, oldRange, newRange);
    }

    // 2) First net decrease, then net increase
    if (!oldRange.isEmpty()) {
      applyNetRemovals(hotelId, oldRange, newRange);
    }
    if (!newRange.isEmpty()) {
      applyNetAdds(hotelId, oldRange, newRange);
    }
  }

  private void precheckNetAddsOrThrow(final Long hotelId,
                                      final Range oldRange,
                                      final Range newRange) {
    // If the room types are the same and the dates are the same, no net
    // increase
    if (oldRange.sameTypeWith(newRange) && oldRange.days.equals(newRange.days)) {
      return;
    }

    // Calculate the "net increase" set
    final boolean sameType = oldRange.sameTypeWith(newRange);
    for (final LocalDate d : newRange.days) {
      final boolean alreadyHeldSameType = sameType && oldRange.days.contains(d);
      if (!alreadyHeldSameType) {
        final RoomTypeInventory inv = getOrInitInventoryRow(hotelId,
            newRange.roomTypeId, d);
        final int available =
            inv.getTotal() - inv.getReserved() - inv.getBlocked();
        if (available <= 0) {
          throw new BadRequestException("No availability on " + d + " for the" +
              " target room type.");
        }
      }
    }
  }

  private void applyNetRemovals(final Long hotelId,
                                final Range oldRange,
                                final Range newRangeOrEmpty) {
    final boolean sameType = oldRange.sameTypeWith(newRangeOrEmpty);
    for (final LocalDate d : oldRange.days) {
      final boolean keep = sameType && newRangeOrEmpty.days.contains(d);
      if (!keep) {
        invRepo.findForUpdate(hotelId, oldRange.roomTypeId, d).ifPresent(inv -> {
          final int reserved = Math.max(0, inv.getReserved() - 1);
          inv.setReserved(reserved);
          inv.setAvailable(inv.getTotal() - reserved - inv.getBlocked());
          invRepo.save(inv);
        });
      }
    }
  }

  private void applyNetAdds(final Long hotelId,
                            final Range oldRangeOrEmpty,
                            final Range newRange) {
    final boolean sameType = oldRangeOrEmpty.sameTypeWith(newRange);
    final List<RoomTypeInventory> toSave = new ArrayList<>();
    for (final LocalDate d : newRange.days) {
      final boolean alreadyHeld = sameType && oldRangeOrEmpty.days.contains(d);
      if (!alreadyHeld) {
        final RoomTypeInventory inv = getOrInitInventoryRow(hotelId,
            newRange.roomTypeId, d);
        inv.setReserved(inv.getReserved() + 1);
        inv.setAvailable(inv.getTotal() - inv.getReserved() - inv.getBlocked());
        toSave.add(inv);
      }
    }
    if (!toSave.isEmpty()) {
      invRepo.saveAll(toSave);
    }
  }

  private Range normalizeRange(final String label,
                               final Long hotelId,
                               @Nullable final Long typeId,
                               @Nullable final LocalDate in,
                               @Nullable final LocalDate out) {
    if (typeId == null && in == null && out == null) {
      return Range.empty();
    }
    // Non-empty parameters must be complete
    if (hotelId == null || typeId == null || in == null || out == null) {
      throw new BadRequestException("Missing " + label + " inventory " +
          "parameters.");
    }
    if (!out.isAfter(in)) {
      throw new BadRequestException("Invalid " + label + " stay date range.");
    }
    // Verify the legality of the room type's ownership
    final RoomTypes rt = roomTypesRepo.findById(typeId)
        .orElseThrow(() -> new BadRequestException("Room type not found: " + typeId));
    // If the room type does not belong to this hotel, report an error
    if (!Objects.equals(rt.getHotelId(), hotelId)) {
      throw new BadRequestException("Room type does not belong to hotel: " + hotelId);
    }

    // Use a HashSet to store dates for ease of difference
    return new Range(typeId, toDaySet(in, out));
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

  private record Range(Long roomTypeId, Set<LocalDate> days) {
      private Range(@Nullable final Long roomTypeId, final Set<LocalDate> days) {
        this.roomTypeId = roomTypeId;
        this.days = days != null ? days : Collections.emptySet();
      }

      static Range empty() {
        return new Range(null, Collections.emptySet());
      }

      boolean isEmpty() {
        return roomTypeId == null || days.isEmpty();
      }

      boolean sameTypeWith(final Range other) {
        return this.roomTypeId != null
            && this.roomTypeId.equals(other.roomTypeId);
      }
    }
}
