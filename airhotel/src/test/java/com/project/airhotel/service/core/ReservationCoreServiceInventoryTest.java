package com.project.airhotel.service.core;

import com.project.airhotel.exception.BadRequestException;
import com.project.airhotel.model.RoomTypeInventory;
import com.project.airhotel.model.RoomTypes;
import com.project.airhotel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
@EntityScan(basePackageClasses = {
    com.project.airhotel.model.RoomTypes.class,
    com.project.airhotel.model.RoomTypeInventory.class
})
@EnableJpaRepositories(basePackageClasses = {
    com.project.airhotel.repository.RoomTypesRepository.class,
    com.project.airhotel.repository.RoomTypeInventoryRepository.class
})
@Import(ReservationCoreServiceInventoryTest.TestConfig.class)
class ReservationCoreServiceInventoryTest {

  @Autowired
  private RoomTypeInventoryRepository invRepo;

  @Autowired
  private RoomTypesRepository roomTypesRepo;

  @Autowired
  private ReservationCoreService core; // Only inventory related methods will be used

  private Long hotelId;
  private Long roomTypeId;

  @BeforeEach
  void setUp() {
    // Construct a house type (note that non-empty fields are satisfied)
    RoomTypes rt = new RoomTypes();
    rt.setHotel_id(100L);
    rt.setCode("DLX");
    rt.setName("Deluxe King");
    rt.setDescription("test");
    rt.setCapacity(2);
    rt.setBase_rate(new BigDecimal("199.00"));
    rt.setTotal_rooms(5);
    // 可选字段
    rt.setRanking(1);
    roomTypesRepo.saveAndFlush(rt);

    hotelId = rt.getHotel_id();
    roomTypeId = rt.getId();
  }

  /** When inventory rows are missing, press total_rooms to automatically rebuild them + 1 room per night. */
  @Test
  void reserve_shouldCreateMissingInventoryRows_andIncrementReserved() {
    LocalDate in = LocalDate.of(2025, 1, 1);
    LocalDate out = LocalDate.of(2025, 1, 3); // 住 2 晚：1/1, 1/2

    core.reserveInventoryOrThrow(hotelId, roomTypeId, in, out);

    RoomTypeInventory d1 = invRepo.findForUpdate(hotelId, roomTypeId, LocalDate.of(2025,1,1)).orElseThrow();
    RoomTypeInventory d2 = invRepo.findForUpdate(hotelId, roomTypeId, LocalDate.of(2025,1,2)).orElseThrow();

    assertEquals(5, d1.getTotal());
    assertEquals(1, d1.getReserved());
    assertEquals(0, d1.getBlocked());
    assertEquals(4, d1.getAvailable());

    assertEquals(5, d2.getTotal());
    assertEquals(1, d2.getReserved());
    assertEquals(0, d2.getBlocked());
    assertEquals(4, d2.getAvailable());
  }

  /** Refusing to oversell and throwing exceptions after full house and not changing inventory */
  @Test
  void reserve_shouldNotOversell_whenNoAvailability() {
    // Turn down the total number of rooms first to make it easier to fill them up quickly
    RoomTypes rt = roomTypesRepo.findById(roomTypeId).orElseThrow();
    rt.setTotal_rooms(2);
    roomTypesRepo.saveAndFlush(rt);

    LocalDate d = LocalDate.of(2025, 1, 1);

    // First Occupancy
    core.reserveInventoryOrThrow(hotelId, roomTypeId, d, d.plusDays(1));
    // Second occupancy → full
    core.reserveInventoryOrThrow(hotelId, roomTypeId, d, d.plusDays(1));

    RoomTypeInventory inv = invRepo.findForUpdate(hotelId, roomTypeId, d).orElseThrow();
    assertEquals(2, inv.getReserved());
    assertEquals(0, inv.getBlocked());
    assertEquals(0, inv.getAvailable());

    // Third occupancy should throw the wrong
    BadRequestException ex = assertThrows(BadRequestException.class,
        () -> core.reserveInventoryOrThrow(hotelId, roomTypeId, d, d.plusDays(1)));
    assertTrue(ex.getMessage().contains("No availability"));

    // Re-check that it still hasn't been incorrectly incremented
    RoomTypeInventory after = invRepo.findForUpdate(hotelId, roomTypeId, d).orElseThrow();
    assertEquals(2, after.getReserved());
    assertEquals(0, after.getAvailable());
  }

  /** Release of stockpiles should backfill reserved-1, available */
  @Test
  void release_shouldDecreaseReserved_andRestoreAvailable() {
    LocalDate in = LocalDate.of(2025, 1, 10);
    LocalDate out = LocalDate.of(2025, 1, 12); // 两晚

    core.reserveInventoryOrThrow(hotelId, roomTypeId, in, out);

    // Release the same date
    core.releaseInventoryRange(hotelId, roomTypeId, in, out);

    RoomTypeInventory d1 = invRepo.findForUpdate(hotelId, roomTypeId, LocalDate.of(2025,1,10)).orElseThrow();
    RoomTypeInventory d2 = invRepo.findForUpdate(hotelId, roomTypeId, LocalDate.of(2025,1,11)).orElseThrow();

    assertEquals(0, d1.getReserved());
    assertEquals(5, d1.getAvailable());

    assertEquals(0, d2.getReserved());
    assertEquals(5, d2.getAvailable());
  }

  /** When a partial inventory line already exists, it should be incremented based on the existing value;
   * missing dates should be built in automatically. */
  @Test
  void reserve_shouldRespectExistingInventoryRow_andInitMissingDate() {
    LocalDate d1 = LocalDate.of(2025, 1, 20);
    LocalDate d2 = LocalDate.of(2025, 1, 21);

    // Pre-create row d1：total=3, reserved=1, blocked=1 → available=1
    RoomTypeInventory preset = RoomTypeInventory.builder()
        .hotel_id(hotelId)
        .room_type_id(roomTypeId)
        .stay_date(d1)
        .total(3)
        .reserved(1)
        .blocked(1)
        .available(1)
        .build();
    invRepo.saveAndFlush(preset);

    // Occupancy d1~d2 (two nights). Expectation:
    // - d1: reserved from 1 -> 2, available from 1 -> 0
    // - d2: auto create total=room_types.total_rooms(=5)，after occupation: reserved=1, available=4
    core.reserveInventoryOrThrow(hotelId, roomTypeId, d1, d2.plusDays(0)); // [20,21) 只占 20 当晚
    // Occupy one more time containing d2
    core.reserveInventoryOrThrow(hotelId, roomTypeId, d1.plusDays(1), d1.plusDays(2)); // [21,22) 占 21 当晚

    RoomTypeInventory day1 = invRepo.findForUpdate(hotelId, roomTypeId, d1).orElseThrow();
    RoomTypeInventory day2 = invRepo.findForUpdate(hotelId, roomTypeId, d2).orElseThrow();

    assertEquals(3, day1.getTotal());
    assertEquals(2, day1.getReserved());
    assertEquals(1, day1.getBlocked());
    assertEquals(0, day1.getAvailable());

    assertEquals(5, day2.getTotal());
    assertEquals(1, day2.getReserved());
    assertEquals(0, day2.getBlocked());
    assertEquals(4, day2.getAvailable());
  }

  // ---- Test configuration: only the required beans are assembled.
  // order/state flow is not tested in ReservationCoreService. ----
  static class TestConfig {
    @org.springframework.context.annotation.Bean
    ReservationCoreService reservationCoreService(RoomTypeInventoryRepository invRepo,
                                                  RoomTypesRepository roomTypesRepo) {
      ReservationsRepository reservationsRepository = Mockito.mock(ReservationsRepository.class);
      ReservationsStatusHistoryRepository historyRepository = Mockito.mock(ReservationsStatusHistoryRepository.class);
      ReservationStatusMachine statusMachine = new ReservationStatusMachine();
      return new ReservationCoreService(reservationsRepository, historyRepository, statusMachine, invRepo, roomTypesRepo);
    }
  }
}
