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
 */
@Service
@RequiredArgsConstructor
public class ReservationInventoryService {

  private final RoomTypeInventoryRepository invRepo;
  private final RoomTypesRepository roomTypesRepo;

  /** 预占库存：对 [checkIn, checkOut) 每天 reserved += 1，available = total - reserved - blocked */
  public void reserveRangeOrThrow(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    ensureParams(hotelId, roomTypeId, checkIn, checkOut);

    List<LocalDate> days = datesBetween(checkIn, checkOut);
    // 先检查再批量保存，避免半成功
    List<RoomTypeInventory> toSave = new ArrayList<>(days.size());
    for (LocalDate d : days) {
      RoomTypeInventory inv = getOrInitInventoryRow(hotelId, roomTypeId, d);
      int available = inv.getTotal() - inv.getReserved() - inv.getBlocked();
      if (available <= 0) {
        throw new BadRequestException("No availability on " + d + " for this room type.");
      }
      inv.setReserved(inv.getReserved() + 1);
      inv.setAvailable(inv.getTotal() - inv.getReserved() - inv.getBlocked());
      toSave.add(inv);
    }
    invRepo.saveAll(toSave);
  }

  /** 释放库存：对 [checkIn, checkOut) 每天 reserved = max(reserved-1, 0) */
  public void releaseRange(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    if (hotelId == null || roomTypeId == null || checkIn == null || checkOut == null) return;

    for (LocalDate d : datesBetween(checkIn, checkOut)) {
      invRepo.findForUpdate(hotelId, roomTypeId, d).ifPresent(inv -> {
        int reserved = Math.max(0, inv.getReserved() - 1);
        inv.setReserved(reserved);
        inv.setAvailable(inv.getTotal() - reserved - inv.getBlocked());
        invRepo.save(inv);
      });
    }
  }

  /** —— 私有工具 —— */

  private void ensureParams(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    if (hotelId == null || roomTypeId == null || checkIn == null || checkOut == null) {
      throw new BadRequestException("Missing inventory parameters.");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new BadRequestException("Invalid stay date range.");
    }
  }

  private RoomTypeInventory getOrInitInventoryRow(Long hotelId, Long roomTypeId, LocalDate stayDate) {
    // 简化：不加行级锁
    return invRepo.findForUpdate(hotelId, roomTypeId, stayDate)
        .orElseGet(() -> {
          RoomTypes rt = roomTypesRepo.findById(roomTypeId)
              .orElseThrow(() -> new BadRequestException("Room type not found: " + roomTypeId));
          if (!Objects.equals(rt.getHotel_id(), hotelId)) {
            throw new BadRequestException("Room type does not belong to hotel: " + hotelId);
          }
          RoomTypeInventory created = RoomTypeInventory.builder()
              .hotel_id(hotelId)
              .room_type_id(roomTypeId)
              .stay_date(stayDate)
              .total(rt.getTotal_rooms())
              .reserved(0)
              .blocked(0)
              .available(rt.getTotal_rooms())
              .build();
          return invRepo.save(created);
        });
  }

  private List<LocalDate> datesBetween(LocalDate startInclusive, LocalDate endExclusive) {
    long days = endExclusive.toEpochDay() - startInclusive.toEpochDay();
    if (days <= 0) return List.of();
    List<LocalDate> res = new ArrayList<>((int) days);
    LocalDate cur = startInclusive;
    for (int i = 0; i < days; i++) {
      res.add(cur);
      cur = cur.plusDays(1);
    }
    return res;
  }
}
