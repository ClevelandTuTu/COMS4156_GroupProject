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
 * Unit tests for ReservationInventoryService. Focus: cover as many branches as
 * possible with clear per-test branch annotations.
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

  // ====================== reserveRangeOrThrow ======================

  @Test
  @DisplayName("reserveRangeOrThrow → ensureParams: Missing inventory "
      + "parameters branch")
  void reserveRange_missingParams_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(null,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex2 = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            null, in, out));
    assertTrue(ex2.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex3 = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, null, out));
    assertTrue(ex3.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex4 = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, null));
    assertTrue(ex4.getMessage().contains(
        "Missing inventory parameters"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("reserveRangeOrThrow → ensureParams: Invalid stay date range "
      + "branch")
  void reserveRange_invalidDateRange_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in;
    final LocalDate outBefore = in.minusDays(1);

    final BadRequestException ex1 = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex1.getMessage().contains("Invalid stay date range"));

    final BadRequestException ex2 = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, outBefore));
    assertTrue(ex2.getMessage().contains("Invalid stay date range"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("reserveRangeOrThrow → existing inventory row path: "
      + "availability <= 0 branch (throws)")
  void reserveRange_existingRow_noAvailability_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);
    final RoomTypeInventory existing = inv(10, 10, 0);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(
            Optional.of(existing));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("No availability"));

    // saveAll should never be called if exception occurs during pre-check
    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(
        any(RoomTypeInventory.class));
    verify(roomTypesRepo, never()).findById(anyLong());
  }

  @Test
  @DisplayName("reserveRangeOrThrow → existing inventory row path: success "
      + "branch (1-night)")
  void reserveRange_existingRow_success_oneNight() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);
    final RoomTypeInventory existing = inv(5, 1, 1);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(
            Optional.of(existing));

    service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID,
        in, out);

    // After increment: reserved = 2, available = 5 - 2 - 1 = 2
    final ArgumentCaptor<List<RoomTypeInventory>> cap =
        ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    final List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(1, saved.size());
    final RoomTypeInventory s = saved.get(0);
    assertEquals(2, s.getReserved());
    assertEquals(2, s.getAvailable());

    // No need to init via roomTypes
    verify(roomTypesRepo, never()).findById(anyLong());
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: "
      + "success branch (2-nights)")
  void reserveRange_initRow_success_twoNights() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(2);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in.plusDays(1)))
        .thenReturn(Optional.empty());

    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(
        Optional.of(roomType(HOTEL_ID, ROOM_TYPE_ID,
            4)));

    when(invRepo.save(any(RoomTypeInventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID
        , in, out);

    final ArgumentCaptor<List<RoomTypeInventory>> cap =
        ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    final List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(2, saved.size());
    saved.forEach(row -> {
      assertEquals(1, row.getReserved());
      assertEquals(3, row.getAvailable());
      assertEquals(4, row.getTotal());
    });

    // room type metadata fetched twice due to two days init (simple path)
    verify(roomTypesRepo, times(2))
        .findById(ROOM_TYPE_ID);
    // init called twice (two dates)
    verify(invRepo, times(2))
        .save(any(RoomTypeInventory.class));
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: room "
      + "type not found branch (throws)")
  void reserveRange_initRow_roomTypeNotFound_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(roomTypesRepo.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.empty());

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("Room type not found"));

    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(any(
        RoomTypeInventory.class));
  }

  @Test
  @DisplayName("reserveRangeOrThrow → create (init) inventory row path: room "
      + "type belongs to other hotel branch (throws)")
  void reserveRange_initRow_roomTypeBelongsToOtherHotel_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(roomTypesRepo.findById(ROOM_TYPE_ID)).thenReturn(
        Optional.of(roomType(999L, ROOM_TYPE_ID,
            3)));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains(
        "Room type does not belong to hotel"));

    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(any(
        RoomTypeInventory.class));
  }

  @Test
  @DisplayName("reserveRangeOrThrow → multi-night: day1 available then day2 "
      + "no-availability → throws and no saveAll")
  void reserveRange_multiNight_partialNoAvailability_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate day2 = in.plusDays(1);
    final LocalDate out = in.plusDays(2);

    final RoomTypeInventory d1 = inv(5, 1, 0);
    d1.setStayDate(in);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in))
        .thenReturn(Optional.of(d1));

    final RoomTypeInventory d2 = inv(3, 3, 0);
    d2.setStayDate(day2);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, day2))
        .thenReturn(Optional.of(d2));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.reserveRangeOrThrow(HOTEL_ID,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex.getMessage().contains("No availability"));

    verify(invRepo, never()).saveAll(anyList());
    verify(invRepo, never()).save(
        any(RoomTypeInventory.class));
    verifyNoInteractions(roomTypesRepo);
  }

  @Test
  @DisplayName("reserveRangeOrThrow → mixed existing and init rows across 3 "
      + "nights → success; room type fetched only for missing date(s)")
  void reserveRange_mixedExistingAndInit_success_threeNights() {
    final LocalDate in = LocalDate.now();
    final LocalDate d2 = in.plusDays(1);
    final LocalDate d3 = in.plusDays(2);
    final LocalDate out = in.plusDays(3);

    final RoomTypeInventory day1 = inv(5, 1, 0);
    day1.setStayDate(in);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(
            Optional.of(day1));

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, d2)).thenReturn(Optional.empty());
    when(roomTypesRepo.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.of(roomType(HOTEL_ID,
            ROOM_TYPE_ID, 6)));
    // init save returns the created entity itself
    when(invRepo.save(any(RoomTypeInventory.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    final RoomTypeInventory day3 = inv(3, 0, 1);
    day3.setStayDate(d3);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, d3)).thenReturn(
            Optional.of(day3));

    service.reserveRangeOrThrow(HOTEL_ID, ROOM_TYPE_ID,
        in, out);

    @SuppressWarnings("unchecked") final
    ArgumentCaptor<List<RoomTypeInventory>> cap =
        ArgumentCaptor.forClass(List.class);
    verify(invRepo).saveAll(cap.capture());
    final List<RoomTypeInventory> saved = cap.getValue();
    assertEquals(3, saved.size());

    final RoomTypeInventory s1 =
        saved.stream().filter(x
            -> in.equals(x.getStayDate())).findFirst().orElseThrow();
    final RoomTypeInventory s2 =
        saved.stream().filter(x
            -> d2.equals(x.getStayDate())).findFirst().orElseThrow();
    final RoomTypeInventory s3 =
        saved.stream().filter(x
            -> d3.equals(x.getStayDate())).findFirst().orElseThrow();

    assertEquals(2, s1.getReserved());
    assertEquals(5 - 2, s1.getAvailable());

    assertEquals(1, s2.getReserved());
    assertEquals(6 - 1, s2.getAvailable());
    assertEquals(6, s2.getTotal());

    assertEquals(1, s3.getReserved());
    assertEquals(3 - 1 - 1, s3.getAvailable());

    verify(roomTypesRepo, times(1))
        .findById(ROOM_TYPE_ID);
    // Init save called exactly once for day2
    verify(invRepo, times(1))
        .save(any(RoomTypeInventory.class));
  }

  // ====================== releaseRange ======================

  @Test
  @DisplayName("releaseRange → ensureParams: Missing inventory parameters "
      + "branch")
  void releaseRange_missingParams_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    final BadRequestException ex1 = assertThrows(
        BadRequestException.class,
        () -> service.releaseRange(null,
            ROOM_TYPE_ID, in, out));
    assertTrue(ex1.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex2 = assertThrows(
        BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID, null,
            in, out));
    assertTrue(ex2.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex3 = assertThrows(
        BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID,
            ROOM_TYPE_ID, null, out));
    assertTrue(ex3.getMessage().contains(
        "Missing inventory parameters"));

    final BadRequestException ex4 = assertThrows(
        BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID,
            ROOM_TYPE_ID, in, null));
    assertTrue(ex4.getMessage().contains(
        "Missing inventory parameters"));

    verifyNoInteractions(invRepo, roomTypesRepo);
  }

  @Test
  @DisplayName("releaseRange → ensureParams: Invalid stay date range branch")
  void releaseRange_invalidDateRange_throws() {
    final LocalDate in = LocalDate.now();
    final LocalDate outSame = in;

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.releaseRange(HOTEL_ID,
            ROOM_TYPE_ID, in, outSame));
    assertTrue(ex.getMessage().contains("Invalid stay date range"));

    verifyNoInteractions(roomTypesRepo);
    verify(invRepo, never()).save(
        any(RoomTypeInventory.class));
  }

  @Test
  @DisplayName("releaseRange → present inventory row path: decrement reserved"
      + " and not below zero (2 days mixed)")
  void releaseRange_presentRows_mixed_decrementAndNoSaveWhenAbsent() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(2);

    final RoomTypeInventory day1 = inv(5, 2, 1);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(
            Optional.of(day1));

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in.plusDays(1)))
        .thenReturn(Optional.empty());

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID
        , in, out);

    final ArgumentCaptor<RoomTypeInventory> cap =
        ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo, times(1))
        .save(cap.capture());
    final RoomTypeInventory saved = cap.getValue();
    assertEquals(1, saved.getReserved());
    assertEquals(3, saved.getAvailable());

    verifyNoInteractions(roomTypesRepo);
  }

  @Test
  @DisplayName("releaseRange → present inventory row path: reserved already 0"
      + " branch (stay at 0)")
  void releaseRange_presentRow_reservedZero_staysZero() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(1);

    final RoomTypeInventory day = inv(7, 0, 2);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in))
        .thenReturn(Optional.of(day));

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID,
        in, out);

    final ArgumentCaptor<RoomTypeInventory> cap =
        ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo).save(cap.capture());
    final RoomTypeInventory saved = cap.getValue();
    assertEquals(0, saved.getReserved());
    assertEquals(7 - 2, saved.getAvailable());
  }

  @Test
  @DisplayName("releaseRange → two days present: both decremented and saved "
      + "once each")
  void releaseRange_bothPresent_savedTwiceWithCorrectValues() {
    final LocalDate in = LocalDate.now();
    final LocalDate d2 = in.plusDays(1);
    final LocalDate out = in.plusDays(2);

    final RoomTypeInventory day1 = inv(5, 2, 1);
    day1.setStayDate(in);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(
            Optional.of(day1));

    final RoomTypeInventory day2 = inv(2, 1, 0);
    day2.setStayDate(d2);
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, d2)).thenReturn(
            Optional.of(day2));

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID,
        in, out);

    final ArgumentCaptor<RoomTypeInventory> cap =
        ArgumentCaptor.forClass(RoomTypeInventory.class);
    verify(invRepo, times(2))
        .save(cap.capture());
    final List<RoomTypeInventory> saved = cap.getAllValues();

    final RoomTypeInventory s1 =
        saved.stream().filter(x -> in.equals(x.getStayDate()))
            .findFirst().orElseThrow();
    final RoomTypeInventory s2 =
        saved.stream().filter(x -> d2.equals(x.getStayDate()))
            .findFirst().orElseThrow();

    assertEquals(1, s1.getReserved());
    assertEquals(5 - 1 - 1, s1.getAvailable());

    assertEquals(0, s2.getReserved());
    assertEquals(2, s2.getAvailable());

    verifyNoInteractions(roomTypesRepo);
  }

  @Test
  @DisplayName("releaseRange → both dates missing: no writes performed")
  void releaseRange_bothMissing_noSaves() {
    final LocalDate in = LocalDate.now();
    final LocalDate out = in.plusDays(2);

    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in)).thenReturn(Optional.empty());
    when(invRepo.findForUpdate(HOTEL_ID,
        ROOM_TYPE_ID, in.plusDays(1)))
        .thenReturn(Optional.empty());

    service.releaseRange(HOTEL_ID, ROOM_TYPE_ID,
        in, out);

    verify(invRepo, never()).save(
        any(RoomTypeInventory.class));
    verifyNoInteractions(roomTypesRepo);
  }

}
