package com.project.airhotel.reservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.airhotel.config.SpringConfig;
import com.project.airhotel.reservation.dto.CreateReservationRequest;
import com.project.airhotel.reservation.dto.ReservationDetailResponse;
import com.project.airhotel.reservation.dto.ReservationSummaryResponse;
import com.project.airhotel.reservation.service.UserReservationService;
import com.project.airhotel.user.service.AuthUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API tests for {@link UserReservationController}, covering validation failures and happy paths
 * with session + OAuth2 principal resolution.
 */
@WebMvcTest(controllers = UserReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserReservationControllerApiTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private UserReservationService service;

  @MockBean
  private AuthUserService authUserService;


  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("POST /reservations missing required fields → 400 validation error with "
      + "field detail")
  void create_missingRequired_returnsBadRequest() throws Exception {
    mvc.perform(post("/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(hasItem(containsString("roomTypeId"))));

    verifyNoInteractions(service);
    verifyNoInteractions(authUserService);
  }

  @Test
  @DisplayName("POST /reservations uses OAuth2 principal fallback when session is empty")
  void create_valid_usesPrincipalAndStoresSession() throws Exception {
    final ReservationDetailResponse resp = new ReservationDetailResponse();
    resp.setId(99L);
    when(service.createReservation(eq(42L), any(CreateReservationRequest.class)))
        .thenReturn(resp);

    final CreateReservationRequest req = new CreateReservationRequest();
    req.setHotelId(1L);
    req.setRoomTypeId(2L);
    req.setCheckInDate(LocalDate.now().plusDays(1));
    req.setCheckOutDate(LocalDate.now().plusDays(2));
    req.setNumGuests(1);
    req.setCurrency("USD");
    req.setPriceTotal(new BigDecimal("100.00"));

    mvc.perform(post("/reservations")
            .sessionAttr(SpringConfig.SESSION_USER_ID, 42L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99));

    final ArgumentCaptor<CreateReservationRequest> cap =
        ArgumentCaptor.forClass(CreateReservationRequest.class);
    verify(service).createReservation(eq(42L), cap.capture());
    verifyNoInteractions(authUserService);

    final CreateReservationRequest sent = cap.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(1, sent.getNumGuests());
    org.junit.jupiter.api.Assertions.assertTrue(
        sent.getCheckOutDate().isAfter(sent.getCheckInDate()));
  }

  @Test
  @DisplayName("GET /reservations uses session user id when present")
  void list_usesSessionUserId() throws Exception {
    final ReservationSummaryResponse summary = new ReservationSummaryResponse();
    summary.setId(7L);
    when(service.listMyReservations(55L)).thenReturn(List.of(summary));

    mvc.perform(get("/reservations")
            .sessionAttr(SpringConfig.SESSION_USER_ID, 55L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(7));

    verify(service).listMyReservations(55L);
    verifyNoInteractions(authUserService);
  }

  @Test
  @DisplayName("DELETE /reservations/{id} returns 204 and calls cancel")
  void cancel_returnsNoContent() throws Exception {
    mvc.perform(delete("/reservations/{id}", 8L)
            .sessionAttr(SpringConfig.SESSION_USER_ID, 77L))
        .andExpect(status().isNoContent());

    verify(service).cancelMyReservation(77L, 8L);
  }
}
