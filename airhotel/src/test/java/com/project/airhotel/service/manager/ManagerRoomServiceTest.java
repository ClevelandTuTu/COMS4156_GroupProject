package com.project.airhotel.service.manager;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import com.project.airhotel.repository.RoomsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ManagerRoomService.
 * Each test indicates which method & branch is exercised.
 */
@ExtendWith(MockitoExtension.class)
class ManagerRoomServiceTest {

  @Mock
  RoomsRepository roomsRepository;

  @Mock
  EntityGuards entityGuards;

  @InjectMocks
  ManagerRoomService service;

  // ---------- helpers ----------
  private Rooms baseRoom() {
    Rooms r = new Rooms();
    r.setId(100L);
    r.setHotelId(1L);
    r.setRoomTypeId(11L);
    r.setRoomNumber("101A");
    r.setFloor(3);
    r.setStatus(RoomStatus.AVAILABLE);
    return r;
  }

  // ===================== listRooms =====================

  @Test
  @DisplayName("listRooms → branch: with status filter (ensureHotelExists + findByHotelIdAndStatus)")
  void listRooms_withStatus() {
    // Testing method: listRooms; branch: status != null
    Long hotelId = 1L;
    List<Rooms> expected = List.of(baseRoom());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(roomsRepository.findByHotelIdAndStatus(hotelId, RoomStatus.AVAILABLE)).thenReturn(expected);

    List<Rooms> out = service.listRooms(hotelId, RoomStatus.AVAILABLE);

    assertEquals(expected, out);
    verify(entityGuards, times(1)).ensureHotelExists(hotelId);
    verify(roomsRepository, times(1)).findByHotelIdAndStatus(hotelId, RoomStatus.AVAILABLE);
  }

  @Test
  @DisplayName("listRooms → branch: without status (ensureHotelExists + findByHotelId)")
  void listRooms_withoutStatus() {
    // Testing method: listRooms; branch: status == null
    Long hotelId = 1L;
    List<Rooms> expected = List.of(baseRoom());

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    when(roomsRepository.findByHotelId(hotelId)).thenReturn(expected);

    List<Rooms> out = service.listRooms(hotelId, null);

    assertEquals(expected, out);
    verify(entityGuards, times(1)).ensureHotelExists(hotelId);
    verify(roomsRepository, times(1)).findByHotelId(hotelId);
  }

  // ===================== createRoom =====================

  @Test
  @DisplayName("createRoom → branch: room number already exists (throws BadRequestException)")
  void createRoom_roomNumberExists_throws() {
    // Testing method: createRoom; branch: duplicate room number
    Long hotelId = 1L;
    RoomsCreateRequest req = mock(RoomsCreateRequest.class);
    when(req.getRoomTypeId()).thenReturn(11L);     // 用到：校验房型归属
    when(req.getRoomNumber()).thenReturn("101A");  // 用到：重复检查

    when(roomsRepository.existsByHotelIdAndRoomNumber(hotelId, "101A")).thenReturn(true);

    assertThrows(BadRequestException.class, () -> service.createRoom(hotelId, req));

    verify(roomsRepository).existsByHotelIdAndRoomNumber(hotelId, "101A");
    verify(roomsRepository, never()).save(any());
  }


  @Test
  @DisplayName("createRoom → branch: happy path with default status (req.status == null)")
  void createRoom_happyPath_defaultStatus() {
    // Testing method: createRoom; branch: req.getStatus() == null (defaults to available)
    Long hotelId = 1L;
    RoomsCreateRequest req = mock(RoomsCreateRequest.class);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getRoomNumber()).thenReturn("101A");
    when(req.getFloor()).thenReturn(5);
    when(req.getStatus()).thenReturn(null);

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId, 11L);
    when(roomsRepository.existsByHotelIdAndRoomNumber(hotelId, "101A")).thenReturn(false);

    ArgumentCaptor<Rooms> cap = ArgumentCaptor.forClass(Rooms.class);
    when(roomsRepository.save(any(Rooms.class))).thenAnswer(inv -> inv.getArgument(0));

    Rooms out = service.createRoom(hotelId, req);

    verify(roomsRepository).save(cap.capture());
    Rooms saved = cap.getValue();
    assertEquals(hotelId, saved.getHotelId());
    assertEquals(11L, saved.getRoomTypeId());
    assertEquals("101A", saved.getRoomNumber());
    assertEquals(5, saved.getFloor());
    assertEquals(RoomStatus.AVAILABLE, saved.getStatus(), "Default status should be 'available'");

    assertSame(saved, out);
  }

  @Test
  @DisplayName("createRoom → branch: happy path with explicit status (req.status != null)")
  void createRoom_happyPath_explicitStatus() {
    // Testing method: createRoom; branch: req.getStatus() != null
    Long hotelId = 1L;
    RoomsCreateRequest req = mock(RoomsCreateRequest.class);
    when(req.getRoomTypeId()).thenReturn(11L);
    when(req.getRoomNumber()).thenReturn("102B");
    when(req.getFloor()).thenReturn(6);
    // use any non-null enum value; even same as default is fine for hitting the branch
    when(req.getStatus()).thenReturn(RoomStatus.AVAILABLE);

    doNothing().when(entityGuards).ensureHotelExists(hotelId);
    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId, 11L);
    when(roomsRepository.existsByHotelIdAndRoomNumber(hotelId, "102B")).thenReturn(false);

    ArgumentCaptor<Rooms> cap = ArgumentCaptor.forClass(Rooms.class);
    when(roomsRepository.save(any(Rooms.class))).thenAnswer(inv -> inv.getArgument(0));

    Rooms out = service.createRoom(hotelId, req);

    verify(roomsRepository).save(cap.capture());
    Rooms saved = cap.getValue();
    assertEquals("102B", saved.getRoomNumber());
    assertEquals(6, saved.getFloor());
    assertEquals(RoomStatus.AVAILABLE, saved.getStatus());
    assertSame(saved, out);
  }

  // ===================== updateRoom =====================

  @Test
  @DisplayName("updateRoom → branch: change room type (ensureRoomTypeInHotelOrThrow then save)")
  void updateRoom_changeRoomType() {
    // Testing method: updateRoom; branch: req.getRoomTypeId() != null
    Long hotelId = 1L, roomId = 100L;
    Rooms r = baseRoom();
    when(entityGuards.getRoomInHotelOrThrow(hotelId, roomId)).thenReturn(r);

    RoomUpdateRequest req = mock(RoomUpdateRequest.class);
    when(req.getRoomTypeId()).thenReturn(22L);

    doNothing().when(entityGuards).ensureRoomTypeInHotelOrThrow(hotelId, 22L);
    when(roomsRepository.save(r)).thenReturn(r);

    Rooms out = service.updateRoom(hotelId, roomId, req);

    assertSame(r, out);
    assertEquals(22L, r.getRoomTypeId());
    verify(entityGuards, times(1)).ensureRoomTypeInHotelOrThrow(hotelId, 22L);
    verify(roomsRepository, times(1)).save(r);
  }

  @Test
  @DisplayName("updateRoom → branch: change room number to the same value (no uniqueness check failure)")
  void updateRoom_changeRoomNumber_same() {
    // Testing method: updateRoom; branch: req.getRoomNumber() equals current -> still sets but no duplicate error
    Long hotelId = 1L, roomId = 100L;
    Rooms r = baseRoom(); // current "101A"
    when(entityGuards.getRoomInHotelOrThrow(hotelId, roomId)).thenReturn(r);

    RoomUpdateRequest req = mock(RoomUpdateRequest.class);
    when(req.getRoomNumber()).thenReturn("101A");

    when(roomsRepository.save(r)).thenReturn(r);

    Rooms out = service.updateRoom(hotelId, roomId, req);

    assertSame(r, out);
    assertEquals("101A", r.getRoomNumber());
    verify(roomsRepository, times(1)).save(r);
  }

  @Test
  @DisplayName("updateRoom → branch: change room number to a different existing number (throws BadRequestException)")
  void updateRoom_changeRoomNumber_toExisting_throws() {
    // Testing method: updateRoom; branch: req.getRoomNumber() != current && existsByHotelIdAndRoomNumber == true -> throws
    Long hotelId = 1L, roomId = 100L;
    Rooms r = baseRoom(); // current "101A"
    when(entityGuards.getRoomInHotelOrThrow(hotelId, roomId)).thenReturn(r);

    RoomUpdateRequest req = mock(RoomUpdateRequest.class);
    when(req.getRoomNumber()).thenReturn("102B");

    when(roomsRepository.existsByHotelIdAndRoomNumber(hotelId, "102B")).thenReturn(true);

    assertThrows(BadRequestException.class, () -> service.updateRoom(hotelId, roomId, req));
    // We do not add "no interaction" checks; positive verifications enough
    verify(roomsRepository, times(1)).existsByHotelIdAndRoomNumber(hotelId, "102B");
  }

  @Test
  @DisplayName("updateRoom → branch: change floor and status then save")
  void updateRoom_changeFloor_andStatus() {
    // Testing method: updateRoom; branch: req.getFloor()!=null && req.getStatus()!=null
    Long hotelId = 1L, roomId = 100L;
    Rooms r = baseRoom();
    when(entityGuards.getRoomInHotelOrThrow(hotelId, roomId)).thenReturn(r);

    RoomUpdateRequest req = mock(RoomUpdateRequest.class);
    when(req.getFloor()).thenReturn(8);
    when(req.getStatus()).thenReturn(RoomStatus.AVAILABLE); // non-null to hit branch

    when(roomsRepository.save(r)).thenReturn(r);

    Rooms out = service.updateRoom(hotelId, roomId, req);

    assertSame(r, out);
    assertEquals(8, r.getFloor());
    assertEquals(RoomStatus.AVAILABLE, r.getStatus());
    verify(roomsRepository, times(1)).save(r);
  }

  // ===================== deleteRoom =====================

  @Test
  @DisplayName("deleteRoom → branch: delegate to guard then delete by id")
  void deleteRoom_deletes() {
    // Testing method: deleteRoom; branch: guard then delete
    Long hotelId = 1L, roomId = 100L;
    Rooms r = baseRoom();
    when(entityGuards.getRoomInHotelOrThrow(hotelId, roomId)).thenReturn(r);

    doNothing().when(roomsRepository).deleteById(r.getId());

    service.deleteRoom(hotelId, roomId);

    verify(entityGuards, times(1)).getRoomInHotelOrThrow(hotelId, roomId);
    verify(roomsRepository, times(1)).deleteById(100L);
  }
}
