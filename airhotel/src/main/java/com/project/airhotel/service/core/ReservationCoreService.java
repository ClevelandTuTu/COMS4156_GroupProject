package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.ReservationsStatusHistory;
import com.project.airhotel.model.RoomTypeInventory;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.model.enums.ReservationStatus;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.ReservationsStatusHistoryRepository;
import com.project.airhotel.repository.RoomTypeInventoryRepository;
import com.project.airhotel.repository.RoomTypesRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class ReservationCoreService {

  private final ReservationsRepository reservationsRepository;
  private final ReservationsStatusHistoryRepository historyRepository;
  private final ReservationStatusMachine statusMachine;
  private final RoomTypeInventoryRepository invRepo;
  private final RoomTypesRepository roomTypesRepo;

  public ReservationCoreService(ReservationsRepository reservationsRepository,
                                ReservationsStatusHistoryRepository historyRepository,
                                ReservationStatusMachine statusMachine,
                                RoomTypeInventoryRepository invRepo,
                                RoomTypesRepository roomTypesRepo) {
    this.reservationsRepository = reservationsRepository;
    this.historyRepository = historyRepository;
    this.statusMachine = statusMachine;
    this.invRepo = invRepo;
    this.roomTypesRepo = roomTypesRepo;
  }

  /** 只做 nights 校验与写入；todo: 价格/库存后续可在此处扩展 */
  @Transactional
  public Reservations recalcNightsOrThrow(Reservations r, LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null) return r;
    int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    if (nights <= 0) throw new BadRequestException("Check out date must be later than check in date.");
    r.setCheck_in_date(checkIn);
    r.setCheck_out_date(checkOut);
    r.setNights(nights);
    return reservationsRepository.save(r);
  }

  /** Unified Status Change + History */
  @Transactional
  public Reservations changeStatus(Reservations r, ReservationStatus to, String reason, Long changedByUserId) {
    ReservationStatus from = r.getStatus();
    if (!statusMachine.canTransit(from, to)) {
      throw new BadRequestException("Illegal status transition: " + from + " -> " + to);
    }
    r.setStatus(to);
    Reservations saved = reservationsRepository.save(r);

    ReservationsStatusHistory h = new ReservationsStatusHistory();
    h.setReservation_id(saved.getId());
    h.setFrom_status(from);
    h.setTo_status(to);
    h.setChanged_at(LocalDateTime.now());
    h.setChanged_by_user_id(changedByUserId);
    h.setReason(reason);
    historyRepository.save(h);
    return saved;
  }

  /** deduct inventory of the reserved room type when creating reservation for every stayed night */
  @Transactional
  public void reserveInventoryOrThrow(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    if (hotelId == null || roomTypeId == null || checkIn == null || checkOut == null) {
      throw new BadRequestException("Missing inventory parameters");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new BadRequestException("Invalid date range");
    }

    // 1) Lock/build all inventory rows for all reserved dates first and do availability checks;
    // pass checks and then write in unison (two-part, avoiding partial writes)
    List<LocalDate> dates = datesBetween(checkIn, checkOut); // [checkIn, checkOut)
    List<RoomTypeInventory> locked = new ArrayList<>(dates.size());

    for (LocalDate d : dates) {
      RoomTypeInventory inv = lockOrInitInventoryRow(hotelId, roomTypeId, d);

      int available = inv.getTotal() - inv.getReserved() - inv.getBlocked();
      if (available <= 0) {
        throw new BadRequestException("No availability on " + d + " for this room type");
      }
      locked.add(inv);
    }

    // 2) True deduction of inventory (reserved += 1; available number recalculated)
    for (RoomTypeInventory inv : locked) {
      inv.setReserved(inv.getReserved() + 1);
      inv.setAvailable(inv.getTotal() - inv.getReserved() - inv.getBlocked());
    }
    invRepo.saveAllAndFlush(locked);
  }

  /** release inventory of the reserved room type for every stay night when cancelling the reservation */
  @Transactional
  public void releaseInventoryRange(Long hotelId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
    if (hotelId == null || roomTypeId == null || checkIn == null || checkOut == null) return;
    for (LocalDate d : datesBetween(checkIn, checkOut)) {
      // Lock rows that already exist and backfill later;
      // if none of the rows exist, they are considered not to need to be backfilled (fault-tolerant)
      invRepo.findForUpdate(hotelId, roomTypeId, d).ifPresent(inv -> {
        int reserved = Math.max(0, inv.getReserved() - 1);
        inv.setReserved(reserved);
        inv.setAvailable(inv.getTotal() - reserved - inv.getBlocked());
        invRepo.save(inv);
      });
    }
  }

  /** 取消（todo: 释放库存/退款等后续再补） */
  @Transactional
  public void cancel(Reservations r, String reason, Long changedByUserId) {
    if (r.getStatus() == ReservationStatus.CANCELED) return;
    if (r.getStatus() == ReservationStatus.CHECKED_OUT) {
      throw new BadRequestException("Reservation already checked out and cannot be cancelled now");
    }
    // Replenish inventory first (to prevent not replenishing when the status is changed and then abnormal)
    releaseInventoryRange(r.getHotel_id(), r.getRoom_type_id(), r.getCheck_in_date(), r.getCheck_out_date());

    r.setCanceled_at(LocalDateTime.now());
    changeStatus(r, ReservationStatus.CANCELED, reason, changedByUserId);
  }

  /** —— tool methods —— */

  private List<LocalDate> datesBetween(LocalDate startInclusive, LocalDate endExclusive) {
    long days = endExclusive.toEpochDay() - startInclusive.toEpochDay();
    if (days <= 0) return List.of();
    return Stream.iterate(startInclusive, d -> d.plusDays(1)).limit(days).toList();
  }

  /**
   * Locked Inventory Row;
   * If the row doesn't exist, find room_types.total_rooms to create the row first and then lock
   * Rely on unique keys + retries to avoid duplicate inserts under concurrency
   */
  private RoomTypeInventory lockOrInitInventoryRow(Long hotelId, Long roomTypeId, LocalDate stayDate) {
    // Try locking the read first
    Optional<RoomTypeInventory> found = invRepo.findForUpdate(hotelId, roomTypeId, stayDate);
    if (found.isPresent()) return found.get();

    // create row according to room_types.total_rooms
    RoomTypes rt = roomTypesRepo.findById(roomTypeId)
        .orElseThrow(() -> new BadRequestException("Room type not found: " + roomTypeId));
    if (!Objects.equals(rt.getHotel_id(), hotelId)) {
      throw new BadRequestException("Room type does not belong to hotel: " + roomTypeId);
    }

    RoomTypeInventory toCreate = RoomTypeInventory.builder()
        .hotel_id(hotelId)
        .room_type_id(roomTypeId)
        .stay_date(stayDate)
        .total(rt.getTotal_rooms())
        .reserved(0)
        .blocked(0)
        .available(rt.getTotal_rooms())
        .build();

    try {
      invRepo.saveAndFlush(toCreate);
    } catch (DataIntegrityViolationException e) {

    }

    // Locked read again (row must exist now)
    return invRepo.findForUpdate(hotelId, roomTypeId, stayDate)
        .orElseThrow(() -> new BadRequestException("Failed to init inventory row for date: " + stayDate));
  }
}