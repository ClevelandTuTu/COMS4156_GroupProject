package com.project.airhotel.common.guard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.project.airhotel.common.exception.BadRequestException;
import com.project.airhotel.common.exception.NotFoundException;
import com.project.airhotel.hotel.repository.HotelsRepository;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.repository.ReservationsRepository;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.domain.Rooms;
import com.project.airhotel.room.repository.RoomTypesRepository;
import com.project.airhotel.room.repository.RoomsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EntityGuards}.
 * <p>
 * Verifies normal behavior and error scenarios for entity existence and ownership validations.
 */
@ExtendWith(MockitoExtension.class)
class EntityGuardsTest {

  private static final Long HOTEL_ID = 1L;
  private static final Long OTHER_HOTEL_ID = 2L;
  private static final Long ROOM_ID = 10L;
  private static final Long RESERVATION_ID = 20L;
  private static final Long ROOM_TYPE_ID = 30L;
  @Mock
  private HotelsRepository hotelsRepository;
  @Mock
  private RoomTypesRepository roomTypesRepository;
  @Mock
  private ReservationsRepository reservationsRepository;
  @Mock
  private RoomsRepository roomsRepository;
  @InjectMocks
  private EntityGuards entityGuards;

  @BeforeEach
  void setUp() {
    // Reserved for future initialization if needed.
  }

  /**
   * ensureHotelExists should complete successfully when the hotel exists.
   */
  @Test
  void ensureHotelExists_hotelExists_shouldNotThrow() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    assertDoesNotThrow(() -> entityGuards.ensureHotelExists(HOTEL_ID));
  }

  /**
   * ensureHotelExists should throw NotFoundException when the hotel does not exist.
   */
  @Test
  void ensureHotelExists_hotelDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(false);

    final NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> entityGuards.ensureHotelExists(HOTEL_ID)
    );

    assertEquals("Hotel does not exist: " + HOTEL_ID, ex.getMessage());
  }

  /**
   * getRoomInHotelOrThrow should return the room when it belongs to the given hotel.
   */
  @Test
  void getRoomInHotelOrThrow_roomBelongsToHotel_shouldReturnRoom() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Rooms room = org.mockito.Mockito.mock(Rooms.class);
    when(room.getHotelId()).thenReturn(HOTEL_ID);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

    final Rooms result = entityGuards.getRoomInHotelOrThrow(HOTEL_ID, ROOM_ID);

    assertSame(room, result);
  }

  /**
   * getRoomInHotelOrThrow should throw NotFoundException when the hotel does not exist.
   */
  @Test
  void getRoomInHotelOrThrow_hotelDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(false);

    assertThrows(
        NotFoundException.class,
        () -> entityGuards.getRoomInHotelOrThrow(HOTEL_ID, ROOM_ID)
    );
  }

  /**
   * getRoomInHotelOrThrow should throw NotFoundException when the room does not exist.
   */
  @Test
  void getRoomInHotelOrThrow_roomDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

    final NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> entityGuards.getRoomInHotelOrThrow(HOTEL_ID, ROOM_ID)
    );

    assertEquals("Room Id does not exist: " + ROOM_ID, ex.getMessage());
  }

  /**
   * getRoomInHotelOrThrow should throw BadRequestException when the room belongs to another hotel.
   */
  @Test
  void getRoomInHotelOrThrow_roomBelongsToOtherHotel_shouldThrowBadRequest() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Rooms room = org.mockito.Mockito.mock(Rooms.class);
    when(room.getHotelId()).thenReturn(OTHER_HOTEL_ID);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> entityGuards.getRoomInHotelOrThrow(HOTEL_ID, ROOM_ID)
    );

    assertEquals("Room does not belong to this hotel.", ex.getMessage());
  }

  /**
   * getReservationInHotelOrThrow should return the reservation when it belongs to the given hotel.
   */
  @Test
  void getReservationInHotelOrThrow_reservationBelongsToHotel_shouldReturnReservation() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Reservations reservation = org.mockito.Mockito.mock(Reservations.class);
    when(reservation.getHotelId()).thenReturn(HOTEL_ID);
    when(reservationsRepository.findById(RESERVATION_ID))
        .thenReturn(Optional.of(reservation));

    final Reservations result = entityGuards.getReservationInHotelOrThrow(
        HOTEL_ID,
        RESERVATION_ID
    );

    assertSame(reservation, result);
  }

  /**
   * getReservationInHotelOrThrow should throw NotFoundException when the hotel does not exist.
   */
  @Test
  void getReservationInHotelOrThrow_hotelDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(false);

    assertThrows(
        NotFoundException.class,
        () -> entityGuards.getReservationInHotelOrThrow(HOTEL_ID, RESERVATION_ID)
    );
  }

  /**
   * getReservationInHotelOrThrow should throw NotFoundException when the reservation does not
   * exist.
   */
  @Test
  void getReservationInHotelOrThrow_reservationDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);
    when(reservationsRepository.findById(RESERVATION_ID))
        .thenReturn(Optional.empty());

    final NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> entityGuards.getReservationInHotelOrThrow(HOTEL_ID, RESERVATION_ID)
    );

    assertEquals(
        "Reservation does not exist:" + " " + RESERVATION_ID,
        ex.getMessage()
    );
  }

  /**
   * getReservationInHotelOrThrow should throw BadRequestException when the reservation belongs to
   * another hotel.
   */
  @Test
  void getReservationInHotelOrThrow_reservationBelongsToOtherHotel_shouldThrowBadRequest() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Reservations reservation = org.mockito.Mockito.mock(Reservations.class);
    when(reservation.getHotelId()).thenReturn(OTHER_HOTEL_ID);
    when(reservationsRepository.findById(RESERVATION_ID))
        .thenReturn(Optional.of(reservation));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> entityGuards.getReservationInHotelOrThrow(HOTEL_ID, RESERVATION_ID)
    );

    assertEquals(
        "This reservation does not belong to this hotel.",
        ex.getMessage()
    );
  }

  /**
   * ensureRoomTypeInHotelOrThrow should complete successfully when the room type belongs to the
   * hotel.
   */
  @Test
  void ensureRoomTypeInHotelOrThrow_roomTypeBelongsToHotel_shouldNotThrow() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final RoomTypes roomType = org.mockito.Mockito.mock(RoomTypes.class);
    when(roomType.getHotelId()).thenReturn(HOTEL_ID);
    when(roomTypesRepository.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.of(roomType));

    assertDoesNotThrow(() ->
        entityGuards.ensureRoomTypeInHotelOrThrow(HOTEL_ID, ROOM_TYPE_ID));
  }

  /**
   * ensureRoomTypeInHotelOrThrow should throw NotFoundException when the hotel does not exist.
   */
  @Test
  void ensureRoomTypeInHotelOrThrow_hotelDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(false);

    assertThrows(
        NotFoundException.class,
        () -> entityGuards.ensureRoomTypeInHotelOrThrow(HOTEL_ID, ROOM_TYPE_ID)
    );
  }

  /**
   * ensureRoomTypeInHotelOrThrow should throw NotFoundException when the room type does not exist.
   */
  @Test
  void ensureRoomTypeInHotelOrThrow_roomTypeDoesNotExist_shouldThrowNotFound() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);
    when(roomTypesRepository.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.empty());

    final NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> entityGuards.ensureRoomTypeInHotelOrThrow(HOTEL_ID, ROOM_TYPE_ID)
    );

    assertEquals(
        "Room type does not exist: " + ROOM_TYPE_ID,
        ex.getMessage()
    );
  }

  /**
   * ensureRoomTypeInHotelOrThrow should throw BadRequestException when the room type belongs to
   * another hotel.
   */
  @Test
  void ensureRoomTypeInHotelOrThrow_roomTypeBelongsToOtherHotel_shouldThrowBadRequest() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final RoomTypes roomType = org.mockito.Mockito.mock(RoomTypes.class);
    when(roomType.getHotelId()).thenReturn(OTHER_HOTEL_ID);
    when(roomTypesRepository.findById(ROOM_TYPE_ID))
        .thenReturn(Optional.of(roomType));

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> entityGuards.ensureRoomTypeInHotelOrThrow(HOTEL_ID, ROOM_TYPE_ID)
    );

    assertEquals(
        "Room type does not belong to hotel " + HOTEL_ID,
        ex.getMessage()
    );
  }

  /**
   * ensureRoomBelongsToHotelAndType should only validate hotel when expected type is null.
   */
  @Test
  void ensureRoomBelongsToHotelAndType_expectedTypeNull_shouldOnlyValidateHotel() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Rooms room = org.mockito.Mockito.mock(Rooms.class);
    when(room.getHotelId()).thenReturn(HOTEL_ID);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

    assertDoesNotThrow(() ->
        entityGuards.ensureRoomBelongsToHotelAndType(HOTEL_ID, ROOM_ID, null));
  }

  /**
   * ensureRoomBelongsToHotelAndType should succeed when the room type matches the expected type.
   */
  @Test
  void ensureRoomBelongsToHotelAndType_typeMatches_shouldNotThrow() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Rooms room = org.mockito.Mockito.mock(Rooms.class);
    when(room.getHotelId()).thenReturn(HOTEL_ID);
    when(room.getRoomTypeId()).thenReturn(ROOM_TYPE_ID);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

    assertDoesNotThrow(() ->
        entityGuards.ensureRoomBelongsToHotelAndType(
            HOTEL_ID,
            ROOM_ID,
            ROOM_TYPE_ID
        )
    );
  }

  /**
   * ensureRoomBelongsToHotelAndType should throw BadRequestException when the room type does not
   * match the expected type.
   */
  @Test
  void ensureRoomBelongsToHotelAndType_typeNotMatch_shouldThrowBadRequest() {
    when(hotelsRepository.existsById(HOTEL_ID)).thenReturn(true);

    final Rooms room = org.mockito.Mockito.mock(Rooms.class);
    when(room.getHotelId()).thenReturn(HOTEL_ID);
    when(room.getRoomTypeId()).thenReturn(ROOM_TYPE_ID);
    when(roomsRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

    final Long expectedOtherTypeId = 999L;

    final BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> entityGuards.ensureRoomBelongsToHotelAndType(
            HOTEL_ID,
            ROOM_ID,
            expectedOtherTypeId
        )
    );

    assertEquals(
        "Room's type does not match expected roomTypeId.",
        ex.getMessage()
    );
  }
}
