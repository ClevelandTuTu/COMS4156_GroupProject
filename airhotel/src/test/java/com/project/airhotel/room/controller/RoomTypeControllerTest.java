package com.project.airhotel.room.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.room.dto.RoomTypeAvailabilityResponse;
import com.project.airhotel.room.service.RoomTypeAvailabilityService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomTypeControllerTest {

  @Mock
  private RoomTypeAvailabilityService availabilityService;

  @InjectMocks
  private RoomTypeController controller;

  @Test
  @DisplayName("getAvailability delegates to service with all params")
  void getAvailability_delegates() {
    final LocalDate checkIn = LocalDate.of(2025, 12, 1);
    final LocalDate checkOut = LocalDate.of(2025, 12, 3);
    final Integer guests = 2;
    final List<RoomTypeAvailabilityResponse> expected = List.of(
        RoomTypeAvailabilityResponse.builder().roomTypeId(1L).build());

    when(availabilityService.getAvailability(9L, checkIn, checkOut, guests))
        .thenReturn(expected);

    final List<RoomTypeAvailabilityResponse> out =
        controller.getAvailability(9L, checkIn, checkOut, guests);

    assertEquals(expected, out);
    verify(availabilityService).getAvailability(eq(9L), eq(checkIn), eq(checkOut), eq(guests));
  }
}
