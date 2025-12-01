package com.project.airhotel.controller;

import com.project.airhotel.model.Hotels;
import com.project.airhotel.service.publicApi.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HotelController (standalone MockMvc, no @MockBean).
 */
class HotelControllerTest {

  private static final String BASE = "/hotels";

  private MockMvc mvc;
  private HotelService hotelService;
  private HotelController controller;

  @BeforeEach
  void setUp() {
    hotelService = mock(HotelService.class);
    controller = new HotelController(hotelService);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  // ---------- /hotels ----------
  @Nested
  @DisplayName("GET " + BASE)
  class GetAllHotels {

    @Test
    void shouldReturnHotelList() throws Exception {
      Hotels h1 = mockHotel(1L, "A");
      Hotels h2 = mockHotel(2L, "B");
      when(hotelService.getAllHotels()).thenReturn(List.of(h1, h2));

      mvc.perform(get(BASE).accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[1].name").value("B"));

      verify(hotelService, times(1)).getAllHotels();
    }

    @Test
    void shouldReturnEmptyArrayWhenNoData() throws Exception {
      when(hotelService.getAllHotels()).thenReturn(List.of());

      mvc.perform(get(BASE))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contentTypeShouldBeJson() throws Exception {
      when(hotelService.getAllHotels()).thenReturn(List.of());

      mvc.perform(get(BASE))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }
  }

  // ---------- /hotels/{id} ----------
  @Nested
  class GetHotelById {

    @Test
    void shouldReturnHotelWithFullAddress() throws Exception {
      Hotels h = mockHotel(2L, "The Ritz-Carlton, SF");
      when(h.getAddressLine1()).thenReturn("600 Stockton St");
      when(h.getAddressLine2()).thenReturn("");
      when(h.getCity()).thenReturn("San Francisco");
      when(h.getState()).thenReturn("CA");
      when(h.getCountry()).thenReturn("US");
      when(h.getPostalCode()).thenReturn("94108");
      when(hotelService.getById(2L)).thenReturn(h);

      mvc.perform(get(BASE + "/{id}", 2))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(2))
          .andExpect(jsonPath("$.name").value("The Ritz-Carlton, SF"))
          .andExpect(jsonPath("$.address").value("600 Stockton St, San Francisco, CA, US 94108"));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
      when(hotelService.getById(anyLong()))
          .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found"));

      mvc.perform(get(BASE + "/{id}", 9999))
          .andExpect(status().isNotFound());
    }

    @Test
    void responseHasOnlyExpectedFields() throws Exception {
      Hotels h = mockHotel(1L, "A");
      when(h.getAddressLine1()).thenReturn("addr1");
      when(h.getCity()).thenReturn("City");
      when(h.getState()).thenReturn("ST");
      when(h.getCountry()).thenReturn("US");
      when(h.getPostalCode()).thenReturn("00000");
      when(hotelService.getById(1L)).thenReturn(h);

      mvc.perform(get(BASE + "/{id}", 1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.name").exists())
          .andExpect(jsonPath("$.address").exists())
          .andExpect(jsonPath("$.totalRooms").doesNotExist())
          .andExpect(jsonPath("$.availableRooms").doesNotExist());
    }
  }

  // ---------- /hotels/{id}/room-types ----------
  @Nested
  @DisplayName("GET " + BASE + "/{id}/room-types")
  class GetRoomTypes {

    @Test
    void shouldReturnRoomTypes() throws Exception {
      Map<String, Object> rt1 = Map.of(
          "roomTypeId", 44L, "code", "SR-5THAVE", "name", "Fifth Avenue Suite",
          "capacity", 3, "bedType", "King", "totalRooms", 8
      );
      Map<String, Object> rt2 = Map.of(
          "roomTypeId", 45L, "code", "SR-IMPERIAL", "name", "Imperial Suite",
          "capacity", 3, "bedType", "King", "totalRooms", 2
      );
      when(hotelService.getRoomTypeAvailability(5L)).thenReturn(List.of(rt1, rt2));

      mvc.perform(get(BASE + "/{id}/room-types", 5))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].code").value("SR-5THAVE"))
          .andExpect(jsonPath("$[1].totalRooms").value(2))
          .andExpect(jsonPath("$[0].priceToday").doesNotExist());
    }

    @Test
    void shouldReturnEmptyWhenNoTypes() throws Exception {
      when(hotelService.getRoomTypeAvailability(7L)).thenReturn(List.of());

      mvc.perform(get(BASE + "/{id}/room-types", 7))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void verifyServiceCalledOnce() throws Exception {
      when(hotelService.getRoomTypeAvailability(3L)).thenReturn(List.of());

      mvc.perform(get(BASE + "/{id}/room-types", 3))
          .andExpect(status().isOk());

      verify(hotelService, times(1)).getRoomTypeAvailability(3L);
    }
  }

  // ---------- /hotels/search ----------

  @Nested
  @DisplayName("GET " + BASE + "/search")
  class SearchHotelsByCity {

    @Test
    void shouldReturnMatchingHotelsForCityKeyword() throws Exception {
      Hotels h1 = mockHotel(5L, "The St. Regis New York");
      when(h1.getCity()).thenReturn("New York");
      Hotels h2 = mockHotel(13L, "JW Marriott Essex House");
      when(h2.getCity()).thenReturn("New York");

      when(hotelService.searchHotelsByCityFuzzy("new"))
          .thenReturn(List.of(h1, h2));

      mvc.perform(get(BASE + "/search")
              .param("city", "new")
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(5))
          .andExpect(jsonPath("$[1].name").value("JW Marriott Essex House"));

      verify(hotelService, times(1)).searchHotelsByCityFuzzy("new");
    }

    @Test
    void shouldReturnEmptyArrayWhenNoHotelMatches() throws Exception {
      when(hotelService.searchHotelsByCityFuzzy("zzz"))
          .thenReturn(List.of());

      mvc.perform(get(BASE + "/search")
              .param("city", "zzz"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));

      verify(hotelService).searchHotelsByCityFuzzy("zzz");
    }

    @Test
    void shouldReturn400WhenServiceThrowsBadRequest() throws Exception {
      when(hotelService.searchHotelsByCityFuzzy("   "))
          .thenThrow(new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "City keyword must not be empty"));

      mvc.perform(get(BASE + "/search")
              .param("city", "   "))
          .andExpect(status().isBadRequest());

      verify(hotelService).searchHotelsByCityFuzzy("   ");
    }
  }

  // ---------- /hotels/search/available ----------

  @Nested
  @DisplayName("GET " + BASE + "/search/available")
  class SearchAvailableHotels {

    @Test
    void shouldReturnAvailableHotelsWhenAllDatesHaveInventory() throws Exception {
      Hotels h = mockHotel(13L, "JW Marriott Essex House New York");
      when(h.getCity()).thenReturn("New York");

      var start = java.time.LocalDate.of(2026, 1, 10);
      var end = java.time.LocalDate.of(2026, 1, 13);

      when(hotelService.searchAvailableHotelsByCityAndDates("new", start, end))
          .thenReturn(List.of(h));

      mvc.perform(get(BASE + "/search/available")
              .param("city", "new")
              .param("startDate", "2026-01-10")
              .param("endDate", "2026-01-13")
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].id").value(13))
          .andExpect(jsonPath("$[0].name")
              .value("JW Marriott Essex House New York"));

      verify(hotelService, times(1))
          .searchAvailableHotelsByCityAndDates("new", start, end);
    }

    @Test
    void shouldReturnEmptyArrayWhenNoHotelAvailable() throws Exception {
      var start = java.time.LocalDate.of(2026, 1, 10);
      var end = java.time.LocalDate.of(2026, 1, 13);

      when(hotelService.searchAvailableHotelsByCityAndDates("new", start, end))
          .thenReturn(List.of());

      mvc.perform(get(BASE + "/search/available")
              .param("city", "new")
              .param("startDate", "2026-01-10")
              .param("endDate", "2026-01-13"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));

      verify(hotelService).searchAvailableHotelsByCityAndDates("new", start, end);
    }

    @Test
    void shouldReturn400WhenServiceRejectsInvalidDates() throws Exception {
      var start = java.time.LocalDate.of(2026, 1, 10);
      var end = java.time.LocalDate.of(2026, 1, 9); // end 在 start 之前

      when(hotelService.searchAvailableHotelsByCityAndDates("new", start, end))
          .thenThrow(new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Invalid start/end date"));

      mvc.perform(get(BASE + "/search/available")
              .param("city", "new")
              .param("startDate", "2026-01-10")
              .param("endDate", "2026-01-09"))
          .andExpect(status().isBadRequest());

      verify(hotelService)
          .searchAvailableHotelsByCityAndDates("new", start, end);
    }
  }

  private static Hotels mockHotel(Long id, String name) {
    Hotels h = mock(Hotels.class);
    when(h.getId()).thenReturn(id);
    when(h.getName()).thenReturn(name);
    return h;
  }
}
