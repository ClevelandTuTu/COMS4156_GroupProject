package com.project.airhotel.reservation.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.project.airhotel.reservation.domain.ReservationChange;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.dto.PatchReservationRequest;
import com.project.airhotel.reservation.dto.ReservationUpdateRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationChangeAdapterTest {

  @Test
  @DisplayName("fromManagerDto maps all editable fields")
  void fromManagerDto_mapsAllFields() {
    final ReservationUpdateRequest req = new ReservationUpdateRequest();
    req.setRoomTypeId(10L);
    req.setRoomId(20L);
    req.setCheckInDate(LocalDate.of(2025, 12, 1));
    req.setCheckOutDate(LocalDate.of(2025, 12, 3));
    req.setNumGuests(2);
    req.setCurrency("USD");
    req.setPriceTotal(new BigDecimal("123.45"));
    req.setNotes("note");
    req.setStatus(ReservationStatus.CONFIRMED);

    final ReservationChange change = ReservationChangeAdapter.fromManagerDto(req);

    assertEquals(10L, change.newRoomTypeId());
    assertEquals(20L, change.newRoomId());
    assertEquals(LocalDate.of(2025, 12, 1), change.newCheckIn());
    assertEquals(LocalDate.of(2025, 12, 3), change.newCheckOut());
    assertEquals(2, change.newNumGuests());
    assertEquals("USD", change.newCurrency());
    assertEquals(new BigDecimal("123.45"), change.newPriceTotal());
    assertEquals("note", change.newNotes());
    assertEquals(ReservationStatus.CONFIRMED, change.newStatus());
  }

  @Test
  @DisplayName("fromUserDto maps only user-editable fields")
  void fromUserDto_mapsLimitedFields() {
    final PatchReservationRequest req = new PatchReservationRequest();
    req.setCheckInDate(LocalDate.of(2025, 11, 1));
    req.setCheckOutDate(LocalDate.of(2025, 11, 4));
    req.setNumGuests(3);

    final ReservationChange change = ReservationChangeAdapter.fromUserDto(req);

    assertEquals(LocalDate.of(2025, 11, 1), change.newCheckIn());
    assertEquals(LocalDate.of(2025, 11, 4), change.newCheckOut());
    assertEquals(3, change.newNumGuests());
    // fields not exposed to users should remain null
    assertNull(change.newRoomTypeId());
    assertNull(change.newRoomId());
    assertNull(change.newCurrency());
    assertNull(change.newPriceTotal());
    assertNull(change.newStatus());
  }
}
