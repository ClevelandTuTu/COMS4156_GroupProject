package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.RoomTypeInventory;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.repository.RoomTypeInventoryRepository;
import com.project.airhotel.repository.RoomTypesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReservationInventoryService.
 * Focus: cover as many branches as possible with clear per-test branch annotations.
 */
@ExtendWith(MockitoExtension.class)
class ReservationInventoryServiceTest {

  @Mock
  private RoomTypeInventoryRepository invRepo;

  @Mock
  private RoomTypesRepository roomTypesRepo;

  @InjectMocks
  private ReservationInventoryService service;

  private static final Long HOTEL_ID = 1L;
  private static final Long ROOM_TYPE_ID = 11L;

  // --- Helpers ---
  private RoomTypeInventory inv(int total, int reserved, int blocked) {
    RoomTypeInventory i = new RoomTypeInventory();
    i.setHotelId(HOTEL_ID);
    i.setRoomTypeId(ROOM_TYPE_ID);
    i.setTotal(total);
    i.setReserved(reserved);
    i.setBlocked(blocked);
    i.setAvailable(total - reserved - blocked);
    return i;
  }

  private RoomTypes roomType(long hotelId, long roomTypeId, int totalRooms) {
    RoomTypes rt = new RoomTypes();
    rt.setHotelId(hotelId);
    rt.setId(roomTypeId);
    rt.setTotalRooms(totalRooms);
    return rt;
  }

  // ====================== reserveRangeOrThrow ======================

  @Test
  @DisplayName("reserveRangeOrThrow → ensureParams: Missing inventory parameters branch")
  void reserveRange_missingParams_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);

    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(null, ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex2 = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, null, in, out));
    assertTrue(ex2.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex3 = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, null, out));
    assertTrue(ex3.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex4 = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, null));
    assertTrue(ex4.getMessage().contains("Missing inventory parameters"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("reserveRangeOrThrow → ensureParams: Invalid stay date range branch")
  void reserveRange_invalidDateRange_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in;              // not after
    LocalDate outBefore = in.minusDays(1);

    BadRequestException ex1 = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out));
    assertTrue(ex1.getMessage().contains("Invalid stay date range"));

    BadRequestException ex2 = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, outBefore));
    assertTrue(ex2.getMessage().contains("Invalid stay date range"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("reserveRangeOrThrow → existing inventory row path: availability <= 0 branch (throws)")
  void reserveRange_existingRow_noAvailability_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);
    RoomTypeInventory existing = inv(10, 10, 0); // available = 0 -> should throw

    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(existing));

    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("No availability"));

    // saveAll should never be called if exception occurs during pre-check
    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(any(RoomTypeInventory.class));
    verify(roomTypesRepo, never()).findById(anyLong());
  }

  @Test
  @DisplayName("reserveRangeOrThrow → existing inventory row path: success branch (1-night)")
  void reserveRange_existingRow_success_oneNight() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);
    RoomTypeInventory existing = inv(5, 1, 1); // available = 3

    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(existing));

    service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out);

    // After increment: reserved = 2, available = 5 - 2 - 1 = 2
    ArgumentCaptor<List<RoomTypeInventory>> cap = ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(1, saved.size());
    RoomTypeInventory s = saved.get(0);
    assertEquals(2, s.getReserved());
    assertEquals(2, s.getAvailable());

    // No need to init via roomTypes
    verify(roomTypesRepo, never()).findById(anyLong());
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: success branch (2-nights)")
  void reserveRange_initRow_success_twoNights() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(2);

    // Simulate that both dates require init (empty on first lookup)
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in.plusDays(1))).thenReturn(Optional.empty());

    // Room type exists and belongs to hotel; total rooms = 4
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 4)));

    // When creating rows
    when(invRepo.save(any(RoomTypeInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out);

    // Each day reserved increments from 0 to 1; available = 4 - 1 - 0 = 3
    ArgumentCaptor<List<RoomTypeInventory>> cap = ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(2, saved.size());
    saved.forEach(row -> {
      assertEquals(1, row.getReserved());
      assertEquals(3, row.getAvailable());
      assertEquals(4, row.getTotal());
    });

    // room type metadata fetched twice due to two days init (simple path)
    verify(roomTypesRepo, times(2)).findById(ROOM_TYPE_ID);
    // init called twice (two dates)
    verify(invRepo, times(2)).save(any(RoomTypeInventory.class));
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: room type not found branch (throws)")
  void reserveRange_initRow_roomTypeNotFound_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);

    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.empty());

    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("Room type not found"));

    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(any(RoomTypeInventory.class));
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: room type belongs to other hotel branch (throws)")
  void reserveRange_initRow_roomTypeBelongsToOtherHotel_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);

    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(999L, ROOM_TYPE_ID, 3)));

    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("Room type does not belong to hotel"));

    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(any(RoomTypeInventory.class));
  }

  // ====================== releaseRange ======================

  @Test
  @DisplayName("releaseRange → ensureParams: Missing inventory parameters branch")
  void releaseRange_missingParams_throws() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);

    BadRequestException ex1 = assertThrows(BadRequestException.class,
        () -> service.releaseRange(null, ROOM_TYPE_ID, in, out));
    assertTrue(ex1.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex2 = assertThrows(BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID, null, in, out));
    assertTrue(ex2.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex3 = assertThrows(BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID, ROOM_TYPE_ID, null, out));
    assertTrue(ex3.getMessage().contains("Missing inventory parameters"));

    BadRequestException ex4 = assertThrows(BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID, ROOM_TYPE_ID, in, null));
    assertTrue(ex4.getMessage().contains("Missing inventory parameters"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("releaseRange → ensureParams: Invalid stay date range branch")
  void releaseRange_invalidDateRange_throws() {
    LocalDate in = LocalDate.now();
    LocalDate outSame = in;

    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID, ROOM_TYPE_ID, in, outSame));
    assertTrue(ex.getMessage().contains("Invalid stay date range"));

    verifyNoInteractions(roomTypesRepo);
    verify(invRepo, never()).save(any(RoomTypeInventory.class));
  }

  @Test
  @DisplayName("releaseRange → present inventory row path: decrement reserved and not below zero (2 days mixed)")
  void releaseRange_presentRows_mixed_decrementAndNoSaveWhenAbsent() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(2);

    // Day 1: present, reserved > 0
    RoomTypeInventory day1 = inv(5, 2, 1); // after release -> reserved=1, available=5-1-1=3
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(day1));

    // Day 2: absent -> Optional.empty() -> no save
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in.plusDays(1))).thenReturn(Optional.empty());

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID, in, out);

    // Verify day1 saved once with decremented reserved
    ArgumentCaptor<RoomTypeInventory> cap = ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo, times(1)).save(cap.capture());
    RoomTypeInventory saved = cap.getValue();
    assertEquals(1, saved.getReserved());
    assertEquals(3, saved.getAvailable());

    // No interactions with roomTypesRepo for release path
    verifyNoInteractions(roomTypesRepo);
  }

  @Test
  @DisplayName("releaseRange → present inventory row path: reserved already 0 branch (stay at 0)")
  void releaseRange_presentRow_reservedZero_staysZero() {
    LocalDate in = LocalDate.now();
    LocalDate out = in.plusDays(1);

    RoomTypeInventory day = inv(7, 0, 2); // already 0
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(day));

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID, in, out);

    ArgumentCaptor<RoomTypeInventory> cap = ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo).save(cap.capture());
    RoomTypeInventory saved = cap.getValue();
    assertEquals(0, saved.getReserved());
    assertEquals(7 - 0 - 2, saved.getAvailable()); // stays consistent
  }
}
