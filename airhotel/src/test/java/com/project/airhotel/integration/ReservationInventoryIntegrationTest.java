package com.project.airhotel.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.airhotel.reservation.service.ReservationInventoryService;
import com.project.airhotel.room.domain.RoomTypeInventory;
import com.project.airhotel.room.domain.RoomTypes;
import com.project.airhotel.room.repository.RoomTypeInventoryRepository;
import com.project.airhotel.room.repository.RoomTypesRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for ReservationInventoryService interacting with JPA repositories.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationInventoryIntegrationTest {

  @Autowired
  private ReservationInventoryService service;

  @Autowired
  private RoomTypesRepository roomTypesRepository;

  @Autowired
  private RoomTypeInventoryRepository inventoryRepository;

  private Long roomTypeId;

  @BeforeEach
  void setUpRoomType() {
    final RoomTypes rt = RoomTypes.builder()
        .hotelId(5L)
        .code("DLX")
        .name("Deluxe")
        .capacity(2)
        .bedType("King")
        .baseRate(new BigDecimal("199.00"))
        .totalRooms(5)
        .build();
    roomTypeId = roomTypesRepository.save(rt).getId();
  }

  @Test
  @DisplayName("applyRangeChangeOrThrow reserves consecutive days and persists inventory rows")
  void applyRangeChange_reserves_and_persists_inventory() {
    final LocalDate checkIn = LocalDate.of(2030, 2, 1);
    final LocalDate checkOut = LocalDate.of(2030, 2, 3); // two nights

    service.applyRangeChangeOrThrow(
        5L,
        null, null, null,
        roomTypeId, checkIn, checkOut);

    final List<RoomTypeInventory> rows =
        inventoryRepository.findByHotelIdAndStayDateBetween(
            5L,
            checkIn,
            checkOut.minusDays(1));

    assertThat(rows).hasSize(2);
    rows.forEach(row -> {
      assertThat(row.getReserved()).isEqualTo(1);
      assertThat(row.getBlocked()).isEqualTo(0);
      assertThat(row.getAvailable()).isEqualTo(4); // total 5 - reserved 1
      assertThat(row.getHotelId()).isEqualTo(5L);
      assertThat(row.getRoomTypeId()).isEqualTo(roomTypeId);
    });
  }
}
