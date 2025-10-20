package com.project.airhotel.guard;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.exception.NotFoundException;
import com.project.airhotel.model.Reservations;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.model.Rooms;
import com.project.airhotel.repository.HotelsRepository;
import com.project.airhotel.repository.ReservationsRepository;
import com.project.airhotel.repository.RoomTypesRepository;
import com.project.airhotel.repository.RoomsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class EntityGuards {
  private final HotelsRepository hotelsRepository;
  private final RoomTypesRepository roomTypesRepository;
  private final ReservationsRepository reservationsRepository;
  private final RoomsRepository roomsRepository;

  public void ensureHotelExists(Long hotelId) {
    if (!hotelsRepository.existsById(hotelId)) {
      throw new NotFoundException("Hotel does not exist: " + hotelId);
    }
  }

  public Rooms getRoomInHotelOrThrow(Long hotelId, Long roomId) {
    ensureHotelExists(hotelId);
    Rooms room = roomsRepository.findById(roomId)
        .orElseThrow(() -> new NotFoundException("Room Id does not exist: " + roomId));
    if (!room.getHotel_id().equals(hotelId)) {
      throw new BadRequestException("Room does not belong to this hotel.");
    }
    return room;
  }

  public Reservations getReservationInHotelOrThrow(Long hotelId, Long reservationId) {
    ensureHotelExists(hotelId);
    Reservations r = reservationsRepository.findById(reservationId)
        .orElseThrow(() -> new NotFoundException("Reservation does not exist: " + reservationId));
    if (!r.getHotel_id().equals(hotelId)) {
      throw new BadRequestException("This reservation does not belong to this hotel.");
    }
    return r;
  }

  public void ensureRoomTypeInHotelOrThrow(Long hotelId, Long roomTypeId) {
    ensureHotelExists(hotelId);
    RoomTypes rt = roomTypesRepository.findById(roomTypeId)
        .orElseThrow(() -> new NotFoundException("Room type does not exist: " + roomTypeId));
    if (!rt.getHotel_id().equals(hotelId)) {
      throw new BadRequestException("Room type does not belong to hotel " + hotelId);
    }
  }

  public void ensureRoomBelongsToHotelAndType(Long hotelId, Long roomId, Long expectedRoomTypeId) {
    Rooms room = getRoomInHotelOrThrow(hotelId, roomId);
    if (expectedRoomTypeId != null && !room.getRoom_type_id().equals(expectedRoomTypeId)) {
      throw new BadRequestException("Room's type does not match expected roomTypeId.");
    }
  }
}
