package com.project.airhotel.service.manager;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.EntityGuards;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.model.enums.RoomStatus;
import com.project.airhotel.repository.RoomsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
@Transactional
public class ManagerRoomService {
  private final RoomsRepository roomsRepository;
  private final EntityGuards entityGuards;

  public ManagerRoomService(final RoomsRepository roomsRepository,
                            final EntityGuards entityGuards) {
    this.roomsRepository = roomsRepository;
    this.entityGuards = entityGuards;
  }

  public List<Rooms> listRooms(final Long hotelId, final RoomStatus status) {
    entityGuards.ensureHotelExists(hotelId);
    if (status != null) {
      return roomsRepository.findByHotelIdAndStatus(hotelId, status);
    }
    return roomsRepository.findByHotelId(hotelId);
  }

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
    r.setHotel_id(hotelId);
    r.setRoom_type_id(req.getRoomTypeId());
    r.setRoom_number(req.getRoomNumber());
    r.setFloor(req.getFloor());
    r.setStatus(req.getStatus() == null ? RoomStatus.available :
        req.getStatus());
    return roomsRepository.save(r);
  }

  public Rooms updateRoom(final Long hotelId, final Long roomId,
                          final RoomUpdateRequest req) {
    final Rooms r = entityGuards.getRoomInHotelOrThrow(hotelId, roomId);

    if (req.getRoomTypeId() != null) {
      entityGuards.ensureRoomTypeInHotelOrThrow(hotelId, req.getRoomTypeId());
      r.setRoom_type_id(req.getRoomTypeId());
    }
    if (req.getRoomNumber() != null) {
      if (!req.getRoomNumber().equals(r.getRoom_number()) &&
          roomsRepository.existsByHotelIdAndRoomNumber(hotelId,
              req.getRoomNumber())) {
        throw new BadRequestException("Room number already exist: "
            + req.getRoomNumber());
      }
      r.setRoom_number(req.getRoomNumber());
    }
    if (req.getFloor() != null) {
      r.setFloor(req.getFloor());
    }
    if (req.getStatus() != null) {
      r.setStatus(req.getStatus());
    }
    return roomsRepository.save(r);
  }

  public void deleteRoom(final Long hotelId, final Long roomId) {
    final Rooms r = entityGuards.getRoomInHotelOrThrow(hotelId, roomId);
    roomsRepository.deleteById(r.getId());
  }
}
