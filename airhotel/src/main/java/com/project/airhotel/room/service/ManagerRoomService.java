package com.project.airhotel.room.service;

import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.room.dto.RoomUpdateRequest;
import com.project.airhotel.room.dto.RoomsCreateRequest;
import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.guard.EntityGuards;
import com.project.airhotel.room.domain.Rooms;
import com.project.airhotel.room.domain.enums.RoomStatus;
import com.project.airhotel.room.repository.RoomsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manager-facing room application service. Provides room listing, creation,
 * update, and deletion operations, with entity boundary checks and basic
 * validation such as room-number uniqueness. All public methods execute within
 * a transactional boundary.
 * <p>
 * Author: Ziyang Su Version: 1.0.0
 */
@Service
@Transactional
public class ManagerRoomService {
  /**
   * Repository used to query, persist, and delete room entities.
   */
  private final RoomsRepository roomsRepository;
  /**
   * Guards that ensure hotel and room type ownership and existence
   * constraints.
   */
  private final EntityGuards entityGuards;

  /**
   * Constructs the manager room service.
   *
   * @param roomsRepo repository for Rooms entities
   * @param eGuards   guards that validate hotel and room-type ownership
   */
  public ManagerRoomService(final RoomsRepository roomsRepo,
                            final EntityGuards eGuards) {
    this.roomsRepository = roomsRepo;
    this.entityGuards = eGuards;
  }

  /**
   * Lists rooms for a given hotel with an optional status filter.
   * <p>
   * Behavior:
   * - Ensures the hotel exists
   * - If status is provided, returns rooms with that status
   * - Otherwise returns all rooms under the hotel
   *
   * @param hotelId the hotel id to list rooms for
   * @param status  optional room status filter, null to list all
   * @return list of matching rooms
   * @throws NotFoundException if the hotel does
   *                                                          not exist
   */
  public List<Rooms> listRooms(final Long hotelId, final RoomStatus status) {
    entityGuards.ensureHotelExists(hotelId);
    if (status != null) {
      return roomsRepository.findByHotelIdAndStatus(hotelId, status);
    }
    return roomsRepository.findByHotelId(hotelId);
  }

  /**
   * Creates a new room under the specified hotel.
   * <p>
   * Validation and side effects:
   * - Ensures the hotel exists
   * - Ensures the room type belongs to the hotel
   * - Enforces room number uniqueness within the hotel
   * - Persists the new room; status defaults to available when not provided
   *
   * @param hotelId the hotel id where the room will be created
   * @param req     creation request containing room type id, room number,
   *                floor, and optional status
   * @return the persisted room entity
   * @throws NotFoundException if hotel or room
   *                                                          type is not found
   * @throws BadRequestException                              if the room number
   *                                                          already exists
   *                                                          within the hotel
   */
  public Rooms createRoom(final Long hotelId, final RoomsCreateRequest req) {
    entityGuards.ensureHotelExists(hotelId);
    entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
    if (roomsRepository.existsByHotelIdAndRoomNumber(hotelId,
        req.getRoomNumber())
    ) {
      throw new BadRequestException("Room number already exists: "
          + req.getRoomNumber());
    }
    final Rooms r = new Rooms();
    r.setHotelId(hotelId);
    r.setRoomTypeId(req.getRoomTypeId());
    r.setRoomNumber(req.getRoomNumber());
    r.setFloor(req.getFloor());
    r.setStatus(req.getStatus() == null ? RoomStatus.AVAILABLE
        : req.getStatus());
    return roomsRepository.save(r);
  }

  /**
   * Updates an existing room that belongs to the given hotel.
   * <p>
   * Supported updates:
   * - roomTypeId: validated to belong to the same hotel
   * - roomNumber: validated for uniqueness within the hotel
   * - floor and status: set directly if provided
   *
   * @param hotelId the hotel id that must own the room
   * @param roomId  the room id to update
   * @param req     update request with fields to modify
   * @return the persisted room entity after the update
   * @throws NotFoundException if hotel or room
   *                                                          is not found
   * @throws BadRequestException                              if the new room
   *                                                          number already
   *                                                          exists in the same
   *                                                          hotel
   */
  public Rooms updateRoom(final Long hotelId, final Long roomId,
                          final RoomUpdateRequest req) {
    final Rooms r = entityGuards.getRoomInHotelOrThrow(hotelId, roomId);

    if (req.getRoomTypeId() != null) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
      r.setRoomTypeId(req.getRoomTypeId());
    }
    if (req.getRoomNumber() != null) {
      if (!req.getRoomNumber().equals(r.getRoomNumber())
          && roomsRepository.existsByHotelIdAndRoomNumber(hotelId,
              req.getRoomNumber())) {
        throw new BadRequestException("Room number already exist: "
            + req.getRoomNumber());
      }
      r.setRoomNumber(req.getRoomNumber());
    }
    if (req.getFloor() != null) {
      r.setFloor(req.getFloor());
    }
    if (req.getStatus() != null) {
      r.setStatus(req.getStatus());
    }
    return roomsRepository.save(r);
  }

  /**
   * Deletes a room that belongs to the given hotel. The room is first resolved
   * and validated through the guards, then deleted by id.
   *
   * @param hotelId the hotel id that must own the room
   * @param roomId  the room id to delete
   * @throws NotFoundException if hotel or room
   *                                                          is not found
   */
  public void deleteRoom(final Long hotelId, final Long roomId) {
    final Rooms r = entityGuards.getRoomInHotelOrThrow(hotelId, roomId);
    roomsRepository.deleteById(r.getId());
  }
}
