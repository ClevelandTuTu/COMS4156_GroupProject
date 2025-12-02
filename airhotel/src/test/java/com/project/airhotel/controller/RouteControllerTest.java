package com.project.airhotel.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.project.airhotel.user.controller.RouteController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET / should return welcome message")
  void indexRootPath() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content()
            .string("Welcome to our hotel reservation App AirHotel!"));
  }

  @Test
  @DisplayName("GET /index should return welcome message")
  void indexAliasPath() throws Exception {
    mockMvc.perform(get("/index"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content()
            .string("Welcome to our hotel reservation App AirHotel!"));
  }
}
