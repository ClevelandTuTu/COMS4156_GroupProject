package com.project.airhotel.reservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.airhotel.reservation.domain.Reservations;
import com.project.airhotel.reservation.domain.enums.ReservationStatus;
import com.project.airhotel.reservation.dto.ApplyUpgradeRequest;
import com.project.airhotel.reservation.dto.ReservationUpdateRequest;
import com.project.airhotel.reservation.service.ManagerReservationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc-based API tests for manager reservation endpoints, covering valid and invalid partitions.
 */
@WebMvcTest(controllers = ManagerReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerReservationControllerApiTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private ManagerReservationService reservationService;

  @Test
  @DisplayName("GET /manager/hotels/{id}/reservations filters by status and dates")
  void list_withFilters_returnsReservations() throws Exception {
    final Reservations r = new Reservations();
    r.setId(10L);
    r.setStatus(ReservationStatus.CONFIRMED);
    when(reservationService.listReservations(
        1L,
        ReservationStatus.CONFIRMED,
        LocalDate.of(2031, 1, 1),
        LocalDate.of(2031, 1, 5)))
        .thenReturn(List.of(r));

    mvc.perform(get("/manager/hotels/{hotelId}/reservations", 1L)
            .param("status", "CONFIRMED")
            .param("start", "2031-01-01")
            .param("end", "2031-01-05"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(10));

    verify(reservationService).listReservations(
        1L,
        ReservationStatus.CONFIRMED,
        LocalDate.of(2031, 1, 1),
        LocalDate.of(2031, 1, 5));
  }

  @Test
  @DisplayName("GET /manager/hotels/{id}/reservations with invalid status returns 400 via handler")
  void list_invalidStatus_returnsBadRequest() throws Exception {
    mvc.perform(get("/manager/hotels/{hotelId}/reservations", 9L)
            .param("status", "BOGUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Allowed values")));

    verifyNoInteractions(reservationService);
  }

  @Test
  @DisplayName("PATCH /manager/hotels/{id}/reservations/{resId} propagates body to service")
  void patch_delegatesWithPayload() throws Exception {
    final Reservations updated = new Reservations();
    updated.setId(5L);
    updated.setStatus(ReservationStatus.CHECKED_IN);
    when(reservationService.patchReservation(eq(2L), eq(5L), any(ReservationUpdateRequest.class)))
        .thenReturn(updated);

    final ReservationUpdateRequest req = new ReservationUpdateRequest();
    req.setStatus(ReservationStatus.CHECKED_IN);

    mvc.perform(patch("/manager/hotels/{hotelId}/reservations/{resId}", 2L, 5L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_IN"));

    verify(reservationService).patchReservation(eq(2L), eq(5L),
        any(ReservationUpdateRequest.class));
  }

  @Test
  @DisplayName("PATCH apply-upgrade with missing roomTypeId triggers validation error from @Valid")
  void applyUpgrade_missingField_returnsBadRequest() throws Exception {
    mvc.perform(patch("/manager/hotels/{hotelId}/reservations/{resId}/apply-upgrade", 2L, 5L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value(containsString("newRoomTypeId")));

    verifyNoInteractions(reservationService);
  }

  @Test
  @DisplayName("DELETE /manager/hotels/{id}/reservations/{resId} returns 204 and calls cancel")
  void cancel_returnsNoContent() throws Exception {
    mvc.perform(delete("/manager/hotels/{hotelId}/reservations/{resId}", 3L, 7L)
            .param("reason", "guest request"))
        .andExpect(status().isNoContent());

    verify(reservationService).cancel(3L, 7L, "guest request");
  }
}
