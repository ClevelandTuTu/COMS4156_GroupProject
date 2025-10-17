package com.project.airhotel.controller;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import com.project.airhotel.service.ManagerRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Validated
@RestController
@RequestMapping("/manager/hotels/{hotelId}/rooms")
public class ManagerRoomController {
  private final ManagerRoomService roomService;


  public ManagerRoomController(ManagerRoomService roomService) {
    this.roomService = roomService;
  }


  /**
   * GET /manager/hotels/{hotelId}/rooms?status=available&page=0&size=20
   */
  @GetMapping
  public List<Rooms> list(
      @PathVariable Long hotelId,
      @RequestParam(required = false) RoomStatus status) {
    return roomService.listRooms(hotelId, status);
  }


  /**
   * POST /manager/hotels/{hotelId}/rooms
   */
  @PostMapping
  public ResponseEntity<Rooms> create(@PathVariable Long hotelId,
                                      @Valid @RequestBody RoomsCreateRequest req) {
    Rooms created = roomService.createRoom(hotelId, req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }


  /**
   * PATCH /manager/hotels/{hotelId}/rooms/{roomId}
   */
  @PatchMapping("/{roomId}")
  public Rooms update(@PathVariable Long hotelId,
                      @PathVariable Long roomId,
                      @Valid @RequestBody RoomUpdateRequest req) {
    return roomService.updateRoom(hotelId, roomId, req);
  }


  /**
   * DELETE /manager/hotels/{hotelId}/rooms/{roomId}
   */
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> delete(@PathVariable Long hotelId,
                                     @PathVariable Long roomId) {
    roomService.deleteRoom(hotelId, roomId);
    return ResponseEntity.noContent().build();
  }
}
