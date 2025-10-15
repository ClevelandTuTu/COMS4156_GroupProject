package com.project.airhotel.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RouteController {

  @GetMapping({"/", "/index"})
  public String index() {
    return "Welcome to our hotel reservation App AirHotel!";
  }

  @GetMapping("/login")
  public String login() {
    return "custom-login"; // resolves to src/main/resources/templates/custom-login.html via Thymeleaf
  }

  // sample protected endpoint
  @GetMapping("/profile")
  @ResponseBody
  public String restricted(OAuth2AuthenticationToken token, Model model) {
    return String.format(
        "Successfully logged in as %s | Name: %s",
        token.getPrincipal().getAttribute("email"),
        token.getPrincipal().getAttribute("name")
    );
  }
}
