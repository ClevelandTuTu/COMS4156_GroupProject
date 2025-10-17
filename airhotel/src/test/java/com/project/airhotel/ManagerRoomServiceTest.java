package com.project.airhotel;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.ManagerEntityGuards;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import com.project.airhotel.repository.RoomsRepository;
import com.project.airhotel.service.ManagerRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class ManagerRoomServiceTest {

  @Mock
  RoomsRepository roomsRepository;
  @Mock
  ManagerEntityGuards guards;

  @InjectMocks
  ManagerRoomService service;

  private Rooms room;

  @BeforeEach
  void setUp() {
    room = new Rooms();
    room.setId(200L);
    room.setHotel_id(2L);
    room.setRoom_type_id(10L);
    room.setRoom_number("1808");
    room.setFloor(18);
    room.setStatus(RoomStatus.available);
  }

  // ---------- listRooms ----------

  @Test
  void listRooms_byStatus() {
    when(roomsRepository.findByHotelIdAndStatus(1L, RoomStatus.available))
        .thenReturn(List.of(room));

    var list = service.listRooms(1L, RoomStatus.available);

    verify(guards).ensureHotelExists(1L);
    verify(roomsRepository).findByHotelIdAndStatus(1L, RoomStatus.available);
    assertThat(list).hasSize(1);
  }

  @Test
  void listRooms_all() {
    when(roomsRepository.findByHotelId(1L)).thenReturn(List.of(room));

    var list = service.listRooms(1L, null);

    verify(guards).ensureHotelExists(1L);
    verify(roomsRepository).findByHotelId(1L);
    assertThat(list).hasSize(1);
  }

  // ---------- createRoom ----------

  @Test
  void createRoom_ok() {
    doNothing().when(guards).ensureHotelExists(1L);
    doNothing().when(guards).ensureRoomTypeExists(10L);
    when(roomsRepository.existsByHotelIdAndRoomNumber(1L, "1808")).thenReturn(false);
    when(roomsRepository.save(any())).thenAnswer(inv -> {
      Rooms r = inv.getArgument(0);
      r.setId(201L);
      return r;
    });

    var req = new RoomsCreateRequest();
    req.setRoomTypeId(10L);
    req.setRoomNumber("1808");
    req.setFloor(18);
    req.setStatus(RoomStatus.available);

    var created = service.createRoom(1L, req);

    assertThat(created.getId()).isEqualTo(201L);
    assertThat(created.getRoom_number()).isEqualTo("1808");
  }

  @Test
  void createRoom_duplicateNumber_throw() {
    doNothing().when(guards).ensureHotelExists(1L);
    doNothing().when(guards).ensureRoomTypeExists(10L);
    when(roomsRepository.existsByHotelIdAndRoomNumber(1L, "1808")).thenReturn(true);

    var req = new RoomsCreateRequest();
    req.setRoomTypeId(10L);
    req.setRoomNumber("1808");

    assertThatThrownBy(() -> service.createRoom(1L, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already exists");
  }

  // ---------- updateRoom ----------

  @Test
  void updateRoom_ok() {
    when(guards.getRoomInHotelOrThrow(1L, 200L)).thenReturn(room);

    doNothing().when(guards).ensureRoomTypeExists(11L);
    when(roomsRepository.existsByHotelIdAndRoomNumber(1L, "1901")).thenReturn(false);
    when(roomsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var req = new RoomUpdateRequest();
    req.setRoomTypeId(11L);
    req.setRoomNumber("1901");
    req.setFloor(19);
    req.setStatus(RoomStatus.maintenance);

    var updated = service.updateRoom(1L, 200L, req);

    assertThat(updated.getRoom_type_id()).isEqualTo(11L);
    assertThat(updated.getRoom_number()).isEqualTo("1901");
    assertThat(updated.getFloor()).isEqualTo(19);
    assertThat(updated.getStatus()).isEqualTo(RoomStatus.maintenance);
  }

  @Test
  void updateRoom_changeNumber_conflict_throw() {
    when(guards.getRoomInHotelOrThrow(1L, 200L)).thenReturn(room);
    when(roomsRepository.existsByHotelIdAndRoomNumber(1L, "1901")).thenReturn(true);

    var req = new RoomUpdateRequest();
    req.setRoomNumber("1901");

    assertThatThrownBy(() -> service.updateRoom(1L, 200L, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already exist");
  }

  // ---------- deleteRoom ----------

  @Test
  void deleteRoom_ok() {
    when(guards.getRoomInHotelOrThrow(1L, 200L)).thenReturn(room);

    service.deleteRoom(1L, 200L);

    verify(roomsRepository).deleteById(200L);
  }
}
