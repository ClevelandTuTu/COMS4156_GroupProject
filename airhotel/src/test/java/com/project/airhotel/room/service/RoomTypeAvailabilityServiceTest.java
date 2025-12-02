package com.project.airhotel.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.dto.RoomTypeAvailabilityResponse;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomTypeAvailabilityServiceTest {

  @Mock
  private RoomTypesRepository roomTypesRepository;

  @Mock
  private RoomTypeInventoryRepository roomTypeInventoryRepository;

  @Mock
  private EntityGuards entityGuards;

  @InjectMocks
  private RoomTypeAvailabilityService service;

  private RoomTypes rt(long id, int capacity, int totalRooms, String baseRate) {
    RoomTypes rt = new RoomTypes();
    rt.setId(id);
    rt.setCapacity(capacity);
    rt.setTotalRooms(totalRooms);
    rt.setBaseRate(new BigDecimal(baseRate));
    rt.setCode("C" + id);
    rt.setName("RT" + id);
    rt.setBedType("King");
    return rt;
  }

  private RoomTypeInventory inv(long roomTypeId, LocalDate date, int available) {
    RoomTypeInventory inv = new RoomTypeInventory();
    inv.setRoomTypeId(roomTypeId);
    inv.setStayDate(date);
    inv.setAvailable(available);
    return inv;
  }

  @Test
  @DisplayName("getAvailability rejects null dates and checkOut<=checkIn")
  void getAvailability_invalidDates() {
    assertThrows(BadRequestException.class,
        () -> service.getAvailability(1L, null, LocalDate.now(), null));
    assertThrows(BadRequestException.class,
        () -> service.getAvailability(1L, LocalDate.now(), null, null));
    assertThrows(BadRequestException.class,
        () -> service.getAvailability(1L, LocalDate.now(), LocalDate.now(), null));
    verify(entityGuards, never()).ensureHotelExists(any());
  }

  @Test
  @DisplayName("getAvailability rejects non-positive numGuests")
  void getAvailability_invalidNumGuests() {
    assertThrows(BadRequestException.class,
        () -> service.getAvailability(1L, LocalDate.now(), LocalDate.now().plusDays(1), 0));
  }

  @Test
  @DisplayName("getAvailability returns empty when no room types")
  void getAvailability_noRoomTypes() {
    when(roomTypesRepository.findByHotelId(1L)).thenReturn(List.of());
    List<RoomTypeAvailabilityResponse> out =
        service.getAvailability(1L, LocalDate.now(), LocalDate.now().plusDays(1), null);
    assertEquals(List.of(), out);
  }

  @Test
  @DisplayName("getAvailability filters by capacity and computes min availability")
  void getAvailability_filtersCapacityAndMin() {
    LocalDate checkIn = LocalDate.of(2026, 1, 1);
    LocalDate checkOut = LocalDate.of(2026, 1, 4); // dates 1,2,3
    when(roomTypesRepository.findByHotelId(9L)).thenReturn(
        List.of(
            rt(1, 2, 5, "100.00"),
            rt(2, 1, 5, "120.00") // filtered out if numGuests=2
        )
    );
    when(roomTypeInventoryRepository.findByHotelIdAndStayDateBetween(
        9L, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of(
            inv(1, LocalDate.of(2026, 1, 1), 4),
            inv(1, LocalDate.of(2026, 1, 2), 2),
            inv(1, LocalDate.of(2026, 1, 3), 3)
        ));

    List<RoomTypeAvailabilityResponse> out =
        service.getAvailability(9L, checkIn, checkOut, 2);

    assertEquals(1, out.size());
    RoomTypeAvailabilityResponse r = out.get(0);
    assertEquals(1L, r.getRoomTypeId());
    assertEquals(2, r.getAvailable()); // min of 4,2,3
  }

  @Test
  @DisplayName("getAvailability defaults missing inventory to totalRooms")
  void getAvailability_missingInventoryUsesTotalRooms() {
    LocalDate checkIn = LocalDate.of(2026, 2, 1);
    LocalDate checkOut = LocalDate.of(2026, 2, 3); // 2 nights
    when(roomTypesRepository.findByHotelId(3L))
        .thenReturn(List.of(rt(10, 4, 7, "50.00")));
    when(roomTypeInventoryRepository.findByHotelIdAndStayDateBetween(
        3L, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of());

    List<RoomTypeAvailabilityResponse> out =
        service.getAvailability(3L, checkIn, checkOut, null);

    assertEquals(1, out.size());
    assertEquals(7, out.get(0).getAvailable());
  }

  @Test
  @DisplayName("getAvailability skips room types with zero min availability")
  void getAvailability_zeroAvailabilitySkips() {
    LocalDate checkIn = LocalDate.of(2026, 3, 1);
    LocalDate checkOut = LocalDate.of(2026, 3, 3); // dates 1,2
    when(roomTypesRepository.findByHotelId(4L))
        .thenReturn(List.of(rt(20, 2, 3, "80.00")));
    when(roomTypeInventoryRepository.findByHotelIdAndStayDateBetween(
        4L, checkIn, checkOut.minusDays(1)))
        .thenReturn(List.of(
            inv(20, LocalDate.of(2026, 3, 1), 1),
            inv(20, LocalDate.of(2026, 3, 2), 0) // drives min to 0
        ));

    List<RoomTypeAvailabilityResponse> out =
        service.getAvailability(4L, checkIn, checkOut, null);

    assertEquals(0, out.size());
  }
}
