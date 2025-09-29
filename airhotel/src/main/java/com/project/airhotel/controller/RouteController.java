package com.project.airhotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RouteController {

  @GetMapping({"/", "/index"})
  public String index() {
    return "Welcome to our hotel reservation App AirHotel!";
  }
}
