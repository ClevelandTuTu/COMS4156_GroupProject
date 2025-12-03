package com.project.airhotel.room.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.airhotel.room.dto.RoomTypeAvailabilityResponse;
import com.project.airhotel.room.service.RoomTypeAvailabilityService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API tests for {@link RoomTypeController}, covering valid and invalid request
 * partitions.
 */
@WebMvcTest(controllers = RoomTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomTypeControllerApiTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RoomTypeAvailabilityService availabilityService;

  @Test
  @DisplayName("GET /hotels/{id}/room-types/availability returns availability when inputs "
      + "are valid")
  void getAvailability_validInputs_returnsList() throws Exception {
    final LocalDate checkIn = LocalDate.of(2026, 1, 1);
    final LocalDate checkOut = LocalDate.of(2026, 1, 3);
    final RoomTypeAvailabilityResponse resp = RoomTypeAvailabilityResponse.builder()
        .roomTypeId(7L)
        .code("DLX")
        .name("Deluxe")
        .available(5)
        .baseRate(new BigDecimal("199.00"))
        .build();
    when(availabilityService.getAvailability(1L, checkIn, checkOut, 2))
        .thenReturn(List.of(resp));

    mvc.perform(get("/hotels/{hotelId}/room-types/availability", 1L)
            .param("checkIn", "2026-01-01")
            .param("checkOut", "2026-01-03")
            .param("numGuests", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].roomTypeId").value(7))
        .andExpect(jsonPath("$[0].available").value(5))
        .andExpect(jsonPath("$[0].code").value("DLX"));

    verify(availabilityService).getAvailability(eq(1L), eq(checkIn), eq(checkOut), eq(2));
  }

  @Test
  @DisplayName("GET /hotels/{id}/room-types/availability missing checkOut → 400 with details")
  void getAvailability_missingCheckout_returnsBadRequest() throws Exception {
    mvc.perform(get("/hotels/{hotelId}/room-types/availability", 2L)
            .param("checkIn", "2026-02-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("checkOut")));

    verifyNoInteractions(availabilityService);
  }

  @Test
  @DisplayName("GET /hotels/{id}/room-types/availability with invalid date format → 400 "
      + "type mismatch")
  void getAvailability_invalidDateFormat_returnsBadRequest() throws Exception {
    mvc.perform(get("/hotels/{hotelId}/room-types/availability", 3L)
            .param("checkIn", "02-01-2026") // invalid format, should be ISO_LOCAL_DATE
            .param("checkOut", "2026-02-05"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("checkIn")))
        .andExpect(jsonPath("$.message").value(containsString("type mismatch")));

    verifyNoInteractions(availabilityService);
  }
}
