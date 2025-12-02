package com.project.airhotel.common.guard;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.hotel.repository.HotelsRepository;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.domain.Rooms;
import com.project.airhotel.room.repository.RoomTypesRepository;
import com.project.airhotel.room.repository.RoomsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Entity boundary guards for domain operations. Centralizes existence checks
 * and ownership validations across hotels, rooms, room types, and reservations.
 * Methods throw NotFoundException for missing entities and BadRequestException
 * for scope or ownership violations.
 */
@Component
@RequiredArgsConstructor
public class EntityGuards {

  /**
   * Repository for hotel existence checks.
   */
  private final HotelsRepository hotelsRepository;

  /**
   * Repository for loading and validating room-type ownership.
   */
  private final RoomTypesRepository roomTypesRepository;

  /**
   * Repository for loading reservations and validating hotel scope.
   */
  private final ReservationsRepository reservationsRepository;

  /**
   * Repository for loading rooms and validating hotel scope.
   */
  private final RoomsRepository roomsRepository;

  /**
   * Ensures that a hotel exists.
   *
   * @param hotelId hotel identifier
   * @throws NotFoundException when the hotel does not exist
   */
  public void ensureHotelExists(final Long hotelId) {
    if (!hotelsRepository.existsById(hotelId)) {
      throw new NotFoundException("Hotel does not exist: " + hotelId);
    }
  }

  /**
   * Resolves a room by id and asserts it belongs to the given hotel.
   *
   * @param hotelId expected owning hotel id
   * @param roomId  room id to resolve
   * @return the resolved room
   * @throws NotFoundException   when the room does not exist or the hotel does
   *                             not exist
   * @throws BadRequestException when the room does not belong to the given
   *                             hotel
   */
  public Rooms getRoomInHotelOrThrow(final Long hotelId, final Long roomId) {
    ensureHotelExists(hotelId);
    final Rooms room = roomsRepository.findById(roomId)
        .orElseThrow(()
            -> new NotFoundException("Room Id does not exist: " + roomId));
    if (!room.getHotelId().equals(hotelId)) {
      throw new BadRequestException("Room does not belong to this hotel.");
    }
    return room;
  }

  /**
   * Resolves a reservation by id and asserts it belongs to the given hotel.
   *
   * @param hotelId       expected owning hotel id
   * @param reservationId reservation id to resolve
   * @return the resolved reservation
   * @throws NotFoundException   when the reservation does not exist or the
   *                             hotel does not exist
   * @throws BadRequestException when the reservation does not belong to the
   *                             given hotel
   */
  public Reservations getReservationInHotelOrThrow(final Long hotelId,
                                                   final Long reservationId) {
    ensureHotelExists(hotelId);
    final Reservations r = reservationsRepository.findById(reservationId)
        .orElseThrow(()
            -> new NotFoundException("Reservation does not exist:"
            + " " + reservationId));
    if (!r.getHotelId().equals(hotelId)) {
      throw new BadRequestException("This reservation does not belong to this"
          + " hotel.");
    }
    return r;
  }

  /**
   * Ensures a room type exists and belongs to the given hotel.
   *
   * @param hotelId    expected owning hotel id
   * @param roomTypeId room type id to validate
   * @throws NotFoundException   when the room type does not exist or the hotel
   *                             does not exist
   * @throws BadRequestException when the room type does not belong to the given
   *                             hotel
   */
  public void ensureRoomTypeInHotelOrThrow(final Long hotelId,
                                           final Long roomTypeId) {
    ensureHotelExists(hotelId);
    final RoomTypes rt = roomTypesRepository.findById(roomTypeId)
        .orElseThrow(()
            -> new NotFoundException("Room type does not exist: "
            + roomTypeId));
    if (!rt.getHotelId().equals(hotelId)) {
      throw new BadRequestException("Room type does not belong to hotel "
          + hotelId);
    }
  }

  /**
   * Ensures a concrete room belongs to the given hotel and, if an expected room
   * type id is provided, that the room's type matches the expected type.
   *
   * @param hotelId            expected owning hotel id
   * @param roomId             room id to validate
   * @param expectedRoomTypeId optional expected room type id; when non-null it
   *                           must match the room's type
   * @throws NotFoundException   when the room or hotel does not exist
   * @throws BadRequestException when the room does not belong to the hotel or
   *                             its type does not match
   */
  public void ensureRoomBelongsToHotelAndType(final Long hotelId,
                                              final Long roomId,
                                              final Long expectedRoomTypeId) {
    final Rooms room = getRoomInHotelOrThrow(hotelId, roomId);
    if (expectedRoomTypeId != null
        && !room.getRoomTypeId().equals(expectedRoomTypeId)) {
      throw new BadRequestException("Room's type does not match expected "
          + "roomTypeId.");
    }
  }
}
