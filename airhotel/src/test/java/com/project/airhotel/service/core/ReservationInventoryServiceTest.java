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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReservationInventoryService. Focus: cover as many branches as
 * possible with clear per-test branch annotations.
 */
@ExtendWith(MockitoExtension.class)
class ReservationInventoryServiceTest {

  private static final Long HOTEL_ID = 1L;
  private static final Long ROOM_TYPE_ID = 11L;
  @Mock
  private RoomTypeInventoryRepository invRepo;
  @Mock
  private RoomTypesRepository roomTypesRepo;
  @InjectMocks
  private ReservationInventoryService service;

  private RoomTypeInventory inv(final int total, final int reserved,
                                final int blocked) {
    final RoomTypeInventory i = new RoomTypeInventory();
    i.setHotelId(HOTEL_ID);
    i.setRoomTypeId(ROOM_TYPE_ID);
    i.setTotal(total);
    i.setReserved(reserved);
    i.setBlocked(blocked);
    i.setAvailable(total - reserved - blocked);
    return i;
  }

  private RoomTypes roomType(final long hotelId, final long roomTypeId,
                             final int totalRooms) {
    final RoomTypes rt = new RoomTypes();
    rt.setHotelId(hotelId);
    rt.setId(roomTypeId);
    rt.setTotalRooms(totalRooms);
    return rt;
  }

  // ================ Validation of normalizeRange via public method
  // ================

  @Test
  @DisplayName("applyRangeChangeOrThrow → both sides empty: no-op")
  void apply_noop_whenBothEmpty() {
    service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null, null, null,
        null);
    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → missing parameters on non-empty " +
      "side throws")
  void apply_missingParams_throws() {
    final LocalDate d = LocalDate.now();
    // missing hotelId/type/dates in old side
    final BadRequestException ex1 = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, d, null
            , null, null, null));
    assertTrue(ex1.getMessage().contains("Missing old inventory parameters"));

    // missing required on new side
    final BadRequestException ex2 = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null,
            ROOM_TYPE_ID, d, null));
    assertTrue(ex2.getMessage().contains("Missing new inventory parameters"));

    verifyNoInteractions(invRepo);
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → invalid date range on either side " +
      "throws")
  void apply_invalidDateRange_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate outSame = in;

    // invalid new
    final BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null,
            ROOM_TYPE_ID, in, outSame));
    assertTrue(ex.getMessage().contains("Invalid new stay date range"));

    // invalid old
    final BadRequestException ex2 = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in,
            outSame, null, null, null));
    assertTrue(ex2.getMessage().contains("Invalid old stay date range"));
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → room type not found / belongs to " +
      "other hotel throws")
  void apply_roomTypeChecks_throw() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    // not found
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.empty());
    final BadRequestException nf = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null,
            ROOM_TYPE_ID, in, out));
    assertTrue(nf.getMessage().contains("Room type not found"));

    // belongs to other hotel
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(999L, ROOM_TYPE_ID, 3)));
    final BadRequestException oh = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null,
            ROOM_TYPE_ID, in, out));
    assertTrue(oh.getMessage().contains("Room type does not belong to hotel"));
  }

  // ================ Precheck no-availability and success paths
  // ================

  @Test
  @DisplayName("applyRangeChangeOrThrow → new one-day existing row has no " +
      "availability → throws")
  void apply_newExisting_noAvailability_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    // normalize(new) will check room type
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 10)));

    // precheck calls getOrInitInventoryRow → we simulate existing row with
    // no availability
    final RoomTypeInventory existing = inv(10, 10, 0);
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(existing));

    final BadRequestException ex = assertThrows(BadRequestException.class,
        () -> service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("No availability"));

    verify(invRepo, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → init new rows for 2 nights → " +
      "reserved++ and saved")
  void apply_initNew_twoNights_success() {
    final LocalDate in = LocalDate.now();
    final LocalDate d2 = in.plusDays(1);
    final LocalDate out = in.plusDays(2);

    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 4)));
    // both days missing initially
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, d2)).thenReturn(Optional.empty());
    // invRepo.save used for initialization
    when(invRepo.save(any(RoomTypeInventory.class))).thenAnswer(inv -> inv.getArgument(0));

    service.applyRangeChangeOrThrow(HOTEL_ID, null, null, null, ROOM_TYPE_ID,
        in, out);

    @SuppressWarnings("unchecked") final ArgumentCaptor<List<RoomTypeInventory>> cap = ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    final List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(2, saved.size());
    saved.forEach(row -> {
      assertEquals(1, row.getReserved());
      assertEquals(3, row.getAvailable()); // 4 - 1 - 0
      assertEquals(4, row.getTotal());
    });

    // Do not assert exact times() on roomTypesRepo.findById due to internal
    // reuse; at least once is fine.
    verify(roomTypesRepo, atLeastOnce()).findById(ROOM_TYPE_ID);
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → mixed old remove + new add over 2 " +
      "nights")
  void apply_mixed_remove_and_add() {
    final LocalDate inOld = LocalDate.now();
    final LocalDate outOld = inOld.plusDays(2); // [d0, d1]
    final LocalDate inNew = inOld.plusDays(1);
    final LocalDate outNew = inOld.plusDays(3); // [d1, d2]

    when(roomTypesRepo.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 5)));

    final RoomTypeInventory d0 = inv(5, 2, 0);
    d0.setStayDate(inOld);
    final RoomTypeInventory d1 = inv(5, 1, 0);
    d1.setStayDate(inOld.plusDays(1));
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, inOld)).thenReturn(Optional.of(d0));

    final LocalDate d2 = inOld.plusDays(2);
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, d2)).thenReturn(Optional.empty());
    when(invRepo.save(any(RoomTypeInventory.class))).thenAnswer(inv -> inv.getArgument(0));

    service.applyRangeChangeOrThrow(HOTEL_ID,
        ROOM_TYPE_ID, inOld, outOld,
        ROOM_TYPE_ID, inNew, outNew);

    final ArgumentCaptor<RoomTypeInventory> singleSaveCap =
        ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo, atLeast(1)).save(singleSaveCap.capture());

    final boolean foundD0Decrement =
        singleSaveCap.getAllValues().stream().anyMatch(x ->
        inOld.equals(x.getStayDate()) && x.getReserved() == 1 && x.getAvailable() == (5 - 1));
    assertTrue(foundD0Decrement, "Day0 should be decremented to reserved=1");

    final boolean wroteD1 = singleSaveCap.getAllValues().stream()
        .anyMatch(x -> inOld.plusDays(1).equals(x.getStayDate()));
    assertFalse(wroteD1, "Day1 should be kept; no save should occur");

    @SuppressWarnings("unchecked") final ArgumentCaptor<List<RoomTypeInventory>> saveAllCap = ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(saveAllCap.capture());
    final RoomTypeInventory addD2 = saveAllCap.getValue().stream()
        .filter(x -> d2.equals(x.getStayDate()))
        .findFirst().orElse(null);
    assertNotNull(addD2);
    assertEquals(1, addD2.getReserved());
    assertEquals(4, addD2.getAvailable());
  }

  // ================ Pure removals ================

  @Test
  @DisplayName("applyRangeChangeOrThrow → pure removal: present rows " +
      "decremented and not below zero")
  void apply_pureRemoval_presentRows() {
    final LocalDate in = LocalDate.now();
    final LocalDate d2 = in.plusDays(1);
    final LocalDate out = in.plusDays(2);

    final RoomTypeInventory day1 = inv(5, 2, 1);
    day1.setStayDate(in);
    final RoomTypeInventory day2 = inv(2, 0, 0);
    day2.setStayDate(d2);

    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 5)));
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.of(day1));
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, d2)).thenReturn(Optional.of(day2));

    service.applyRangeChangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out, null,
        null, null);

    final ArgumentCaptor<RoomTypeInventory> cap =
        ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo, times(2)).save(cap.capture());
    final List<RoomTypeInventory> saved = cap.getAllValues();

    final RoomTypeInventory s1 =
        saved.stream().filter(x -> in.equals(x.getStayDate())).findFirst().orElseThrow();
    assertEquals(1, s1.getReserved());
    assertEquals(3, s1.getAvailable()); // 5 - 1 - 1

    final RoomTypeInventory s2 =
        saved.stream().filter(x -> d2.equals(x.getStayDate())).findFirst().orElseThrow();
    assertEquals(0, s2.getReserved()); // already 0, stays 0
    assertEquals(2, s2.getAvailable()); // 2 - 0 - 0
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow → removal when both days missing does" +
      " not write")
  void apply_removal_missingRows_noWrites() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(2);

    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID, 5)));
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(invRepo.findForUpdate(HOTEL_ID, ROOM_TYPE_ID, in.plusDays(1))).thenReturn(Optional.empty());

    service.applyRangeChangeOrThrow(HOTEL_ID, ROOM_TYPE_ID, in, out, null,
        null, null);

    verify(invRepo, never()).save(any(RoomTypeInventory.class));
    verify(invRepo, never()).saveAll(anyList());
  }
}
