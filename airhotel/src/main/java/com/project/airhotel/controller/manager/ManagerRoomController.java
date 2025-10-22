package com.project.airhotel.controller.manager;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import com.project.airhotel.service.manager.ManagerRoomService;
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
 * Manager-facing REST controller for room operations. Exposes endpoints to
 * list, create, update, and delete rooms under a specific hotel. All business
 * logic is delegated to ManagerRoomService.
 * <p>
 * Base path: /manager/hotels/{hotelId}/rooms
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Validated
@RestController
@RequestMapping("/manager/hotels/{hotelId}/rooms")
public class ManagerRoomController {

  /**
   * Application service for manager-side room use cases.
   */
  private final ManagerRoomService roomService;


  /**
   * Constructs the controller with its required service dependency.
   *
   * @param roomServ manager room application service
   */
  public ManagerRoomController(final ManagerRoomService roomServ) {
    this.roomService = roomServ;
  }


  /**
   * Lists rooms for a hotel with an optional status filter.
   * <p>
   * GET /manager/hotels/{hotelId}/rooms Example: GET
   * /manager/hotels/{hotelId}/rooms?status=available
   *
   * @param hotelId hotel identifier
   * @param status  optional room status filter; when null returns all rooms
   * @return list of rooms matching the filter
   */
  @GetMapping
  public List<Rooms> list(
      @PathVariable final Long hotelId,
      @RequestParam(required = false) final RoomStatus status) {
    return roomService.listRooms(hotelId, status);
  }


  /**
   * Creates a new room under the given hotel.
   * <p>
   * POST /manager/hotels/{hotelId}/rooms
   *
   * @param hotelId hotel identifier
   * @param req     creation payload containing room type id, room number,
   *                floor, and optional status
   * @return 201 Created with the persisted room as body
   */
  @PostMapping
  public ResponseEntity<Rooms> create(
      @PathVariable final Long hotelId,
      @Valid @RequestBody final RoomsCreateRequest req) {
    final Rooms created = roomService.createRoom(hotelId, req);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }


  /**
   * Partially updates an existing room that belongs to the hotel.
   * <p>
   * PATCH /manager/hotels/{hotelId}/rooms/{roomId}
   *
   * @param hotelId hotel identifier
   * @param roomId  room identifier
   * @param req     update payload with fields to modify
   * @return updated room entity
   */
  @PatchMapping("/{roomId}")
  public Rooms update(@PathVariable final Long hotelId,
                      @PathVariable final Long roomId,
                      @Valid @RequestBody final RoomUpdateRequest req) {
    return roomService.updateRoom(hotelId, roomId, req);
  }


  /**
   * Deletes a room that belongs to the hotel. Returns 204 No Content on
   * success.
   * <p>
   * DELETE /manager/hotels/{hotelId}/rooms/{roomId}
   *
   * @param hotelId hotel identifier
   * @param roomId  room identifier
   * @return empty 204 response on success
   */
  @DeleteMapping("/{roomId}")
  public ResponseEntity<Void> delete(@PathVariable final Long hotelId,
                                     @PathVariable final Long roomId) {
    roomService.deleteRoom(hotelId, roomId);
    return ResponseEntity.noContent().build();
  }
}
