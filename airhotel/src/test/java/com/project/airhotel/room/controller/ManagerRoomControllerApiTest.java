package com.project.airhotel.room.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.airhotel.room.domain.Rooms;
import com.project.airhotel.room.domain.enums.RoomStatus;
import com.project.airhotel.room.dto.RoomsCreateRequest;
import com.project.airhotel.room.service.ManagerRoomService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API-level tests for {@link ManagerRoomController}. Covers successful paths and
 * validation/enum binding failures without touching legacy unit tests.
 */
@WebMvcTest(controllers = ManagerRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerRoomControllerApiTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private ManagerRoomService roomService;

  @Test
  @DisplayName("GET /manager/hotels/{hotelId}/rooms?status=MAINTENANCE returns filtered rooms")
  void list_withStatusFilter_returnsRooms() throws Exception {
    final Rooms r = Rooms.builder()
        .id(10L)
        .hotelId(2L)
        .roomTypeId(50L)
        .roomNumber("502")
        .floor(5)
        .status(RoomStatus.MAINTENANCE)
        .build();
    when(roomService.listRooms(2L, RoomStatus.MAINTENANCE)).thenReturn(List.of(r));

    mvc.perform(get("/manager/hotels/{hotelId}/rooms", 2L)
            .param("status", "MAINTENANCE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].roomNumber").value("502"))
        .andExpect(jsonPath("$[0].status").value("MAINTENANCE"));

    verify(roomService).listRooms(2L, RoomStatus.MAINTENANCE);
  }

  @Test
  @DisplayName("GET /manager/hotels/{hotelId}/rooms with invalid status enum returns 400")
  void list_invalidStatus_returnsBadRequest() throws Exception {
    mvc.perform(get("/manager/hotels/{hotelId}/rooms", 3L)
            .param("status", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Allowed values")));

    verifyNoInteractions(roomService);
  }

  @Test
  @DisplayName("POST /manager/hotels/{hotelId}/rooms missing roomTypeId → 400 validation error")
  void create_missingRoomTypeId_returnsBadRequest() throws Exception {
    final Map<String, Object> payload = Map.of(
        "roomNumber", "101A",
        "floor", 1,
        "status", "AVAILABLE"
    );

    mvc.perform(post("/manager/hotels/{hotelId}/rooms", 1L)
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("roomTypeId")));

    verifyNoInteractions(roomService);
  }

  @Test
  @DisplayName("POST /manager/hotels/{hotelId}/rooms with valid payload returns 201 and "
      + "echoes fields")
  void create_validPayload_returnsCreatedRoom() throws Exception {
    final Rooms created = Rooms.builder()
        .id(99L)
        .hotelId(5L)
        .roomTypeId(20L)
        .roomNumber("201A")
        .floor(2)
        .status(RoomStatus.AVAILABLE)
        .build();
    when(roomService.createRoom(eq(5L), any(RoomsCreateRequest.class)))
        .thenReturn(created);

    final Map<String, Object> payload = Map.of(
        "roomTypeId", 20,
        "roomNumber", "201A",
        "floor", 2,
        "status", "AVAILABLE"
    );

    mvc.perform(post("/manager/hotels/{hotelId}/rooms", 5L)
            .contentType(APPLICATION_JSON)
            .content(mapper.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.roomTypeId").value(20))
        .andExpect(jsonPath("$.roomNumber").value("201A"))
        .andExpect(jsonPath("$.status").value("AVAILABLE"));

    final ArgumentCaptor<RoomsCreateRequest> reqCap =
        ArgumentCaptor.forClass(RoomsCreateRequest.class);
    verify(roomService).createRoom(eq(5L), reqCap.capture());
    final RoomsCreateRequest sent = reqCap.getValue();
    // Ensure equivalence partition of "all fields present" maps through the binder
    org.junit.jupiter.api.Assertions.assertEquals(20L, sent.getRoomTypeId());
    org.junit.jupiter.api.Assertions.assertEquals("201A", sent.getRoomNumber());
    org.junit.jupiter.api.Assertions.assertEquals(2, sent.getFloor());
  }
}
