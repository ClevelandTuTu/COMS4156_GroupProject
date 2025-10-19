package com.project.airhotel.service.manager;

import com.project.airhotel.dto.rooms.RoomUpdateRequest;
import com.project.airhotel.dto.rooms.RoomsCreateRequest;
import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.guard.ManagerEntityGuards;
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
  private final ManagerEntityGuards managerEntityGuards;

  public ManagerRoomService(RoomsRepository roomsRepository,
                            ManagerEntityGuards managerEntityGuards) {
    this.roomsRepository = roomsRepository;
    this.managerEntityGuards = managerEntityGuards;
  }

  public List<Rooms> listRooms(Long hotelId, RoomStatus status) {
    managerEntityGuards.ensureHotelExists(hotelId);
    if (status != null) {
      return roomsRepository.findByHotelIdAndStatus(hotelId, status);
    }
    return roomsRepository.findByHotelId(hotelId);
  }

  public Rooms createRoom(Long hotelId, RoomsCreateRequest req) {
    managerEntityGuards.ensureHotelExists(hotelId);
    managerEntityGuards.ensureRoomTypeExists(req.getRoomTypeId());
    if (roomsRepository.existsByHotelIdAndRoomNumber(hotelId, req.getRoomNumber())
    ) {
      throw new BadRequestException("Room number already exists: " + req.getRoomNumber());
    }
    Rooms r = new Rooms();
    r.setHotel_id(hotelId);
    r.setRoom_type_id(req.getRoomTypeId());
    r.setRoom_number(req.getRoomNumber());
    r.setFloor(req.getFloor());
    r.setStatus(req.getStatus() == null ? RoomStatus.available : req.getStatus());
    return roomsRepository.save(r);
  }

  public Rooms updateRoom(Long hotelId, Long roomId, RoomUpdateRequest req) {
    Rooms r = managerEntityGuards.getRoomInHotelOrThrow(hotelId, roomId);

    if (req.getRoomTypeId() != null) {
      managerEntityGuards.ensureRoomTypeExists(req.getRoomTypeId());
      r.setRoom_type_id(req.getRoomTypeId());
    }
    if (req.getRoomNumber() != null) {
      if (!req.getRoomNumber().equals(r.getRoom_number()) &&
          roomsRepository.existsByHotelIdAndRoomNumber(hotelId, req.getRoomNumber())) {
        throw new BadRequestException("Room number already exist: " + req.getRoomNumber());
      }
      r.setRoom_number(req.getRoomNumber());
    }
    if (req.getFloor() != null) r.setFloor(req.getFloor());
    if (req.getStatus() != null) r.setStatus(req.getStatus());
    return roomsRepository.save(r);
  }

  public void deleteRoom(Long hotelId, Long roomId) {
    Rooms r = managerEntityGuards.getRoomInHotelOrThrow(hotelId, roomId);
    roomsRepository.deleteById(r.getId());
  }
}