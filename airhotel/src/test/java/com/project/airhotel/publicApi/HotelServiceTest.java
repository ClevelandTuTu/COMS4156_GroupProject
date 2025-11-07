package com.project.airhotel.publicApi;

import com.project.airhotel.hotel.domain.Hotels;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.hotel.repository.HotelsRepository;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import com.project.airhotel.room.repository.RoomsRepository;
import com.project.airhotel.hotel.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HotelService (public API layer).
 */
class HotelServiceTest {

  private HotelsRepository hotelsRepo;
  private RoomsRepository roomsRepo;
  private RoomTypesRepository roomTypesRepo;
  private RoomTypeInventoryRepository roomTypeInventoryRepo;

  private HotelService hotelService;

  @BeforeEach
  void setUp() {
    hotelsRepo = mock(HotelsRepository.class);
    roomsRepo = mock(RoomsRepository.class);
    roomTypesRepo = mock(RoomTypesRepository.class);
    roomTypeInventoryRepo = mock(RoomTypeInventoryRepository.class);

    hotelService = new HotelService(
        hotelsRepo, roomsRepo, roomTypesRepo, roomTypeInventoryRepo);
  }

  // ---------- getById ----------

  @Test
  void getById_validHotel() {
    var h = new Hotels();
    when(hotelsRepo.findById(2L)).thenReturn(Optional.of(h));

    Hotels result = hotelService.getById(2L);

    assertNotNull(result);
    verify(hotelsRepo, times(1)).findById(2L);
  }

  @Test
  void getById_notFound() {
    when(hotelsRepo.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class,
        () -> hotelService.getById(999L));

    verify(hotelsRepo, times(1)).findById(999L);
  }

  // ---------- countRooms ----------

  @Test
  void countRooms_ok() {
    when(roomsRepo.countByHotelId(5L)).thenReturn(10L);

    assertEquals(10L, hotelService.countRooms(5L));
    verify(roomsRepo, times(1)).countByHotelId(5L);
  }

  // ---------- getRoomTypeAvailability ----------

  @Test
  void roomTypeAvailability_withInventory() {
    long hotelId = 5L;
    LocalDate today = LocalDate.now();

    when(hotelsRepo.existsById(hotelId)).thenReturn(true);

    RoomTypes rt = RoomTypes.builder()
        .id(44L)
        .hotelId(hotelId)
        .code("SR-5THAVE")
        .name("Fifth Avenue Suite")
        .capacity(3)
        .bedType("King")
        .bedNum(1)
        .bedSizeM2(new BigDecimal("3.90"))
        .baseRate(new BigDecimal("2600.00"))
        .ranking(85)
        .totalRooms(8)
        .build();
    when(roomTypesRepo.findByHotelId(hotelId)).thenReturn(List.of(rt));

    RoomTypeInventory inv = RoomTypeInventory.builder()
        .id(1L)
        .hotelId(hotelId)
        .roomTypeId(rt.getId())
        .stayDate(today)
        .total(8)
        .reserved(4)
        .blocked(1)
        .available(3)
        .build();
    when(roomTypeInventoryRepo.findByHotelIdAndStayDate(hotelId, today))
        .thenReturn(List.of(inv));

    List<Map<String, Object>> result = hotelService.getRoomTypeAvailability(hotelId);

    assertEquals(1, result.size());
    Map<String, Object> m = result.get(0);
    assertEquals(44L, m.get("roomTypeId"));
    assertEquals("SR-5THAVE", m.get("code"));
    assertEquals("Fifth Avenue Suite", m.get("name"));
    assertEquals("King", m.get("bedType"));
    assertEquals(3, m.get("capacity"));
    assertEquals(8, m.get("totalRooms"));
    assertEquals(3, m.get("available"));

    verify(hotelsRepo, times(1)).existsById(hotelId);
    verify(roomTypesRepo, times(1)).findByHotelId(hotelId);
    verify(roomTypeInventoryRepo, times(1))
        .findByHotelIdAndStayDate(hotelId, today);
  }

  @Test
  void roomTypeAvailability_noInventory() {
    long hotelId = 5L;
    LocalDate today = LocalDate.now();

    when(hotelsRepo.existsById(hotelId)).thenReturn(true);

    RoomTypes rt = RoomTypes.builder()
        .id(6L).hotelId(hotelId).code("SR-DIOR")
        .name("Dior Suite").capacity(3).bedType("King")
        .baseRate(new BigDecimal("4500.00"))
        .totalRooms(1)
        .build();
    when(roomTypesRepo.findByHotelId(hotelId)).thenReturn(List.of(rt));

    when(roomTypeInventoryRepo.findByHotelIdAndStayDate(hotelId, today))
        .thenReturn(List.of());

    List<Map<String, Object>> result = hotelService.getRoomTypeAvailability(hotelId);

    assertEquals(1, result.size());
    assertEquals(0, result.get(0).get("available"));
  }

  @Test
  void roomTypeAvailability_hotelNotExists() {
    when(hotelsRepo.existsById(9L)).thenReturn(false);

    assertThrows(ResponseStatusException.class,
        () -> hotelService.getRoomTypeAvailability(9L));

    verify(hotelsRepo, times(1)).existsById(9L);
    verifyNoInteractions(roomTypesRepo, roomTypeInventoryRepo);
  }
}
