package com.project.airhotel.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class RouteController {

  @GetMapping({"/", "/index"})
  @ResponseBody
  public String index() {
    return "Welcome to our hotel reservation App AirHotel!";
  }

  @GetMapping("/login")
  public String login() {
    return "custom-login"; // resolves to src/main/resources/templates/custom-login.html via Thymeleaf
  }

  @GetMapping("/logout-success")
  @ResponseBody
  public String logoutSuccess() {
    return "You've been logged out successfully!"; // resolves to src/main/resources/templates/custom-login.html via Thymeleaf
  }

  // sample protected endpoint
  @GetMapping("/profile")
  public String restricted(OAuth2AuthenticationToken token, Model model) {
    model.addAttribute("email", token.getPrincipal().getAttribute("email"));
    model.addAttribute("name", token.getPrincipal().getAttribute("name"));
    return "profile";
  }
}
