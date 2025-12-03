package com.project.airhotel.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Routes for basic site navigation and simple examples. Provides a public home
 * page, custom login view, logout success message, and a sample protected
 * profile page that reads attributes from the OAuth2 principal.
 */
@Controller
public class RouteController {

  /**
   * Public welcome endpoint.
   * GET / or /index
   *
   * @return a plain text welcome message
   */
  @GetMapping({"/", "/index"})
  @ResponseBody
  public String index() {
    return "Welcome to our hotel reservation App AirHotel!";
  }

}

