package com.project.airhotel.user.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
   * <p>
   * GET / or /index
   *
   * @return a plain text welcome message
   */
  @GetMapping({"/", "/index"})
  @ResponseBody
  public String index() {
    return "Welcome to our hotel reservation App AirHotel!";
  }

  /**
   * Custom login page for Spring Security. The view name resolves to
   * src/main/resources/templates/custom-login.html via Thymeleaf.
   * <p>
   * GET /login
   *
   * @return the login view name
   */
  @GetMapping("/login")
  public String login() {
    return "custom-login"; // resolves to src/main/resources/templates/custom
    // -login.html via Thymeleaf
  }

  /**
   * Logout success endpoint. Returns a plain text confirmation after the user
   * logs out.
   * <p>
   * GET /logout-success
   *
   * @return a plain text logout confirmation
   */
  @GetMapping("/logout-success")
  @ResponseBody
  public String logoutSuccess() {
    return "You've been logged out successfully!"; // resolves to
    // src/main/resources/templates/custom-login.html via Thymeleaf
  }

  /**
   * Sample protected endpoint that displays user profile information from the
   * OAuth2AuthenticationToken. Requires an authenticated session. The view name
   * resolves to src/main/resources/templates/profile.html via Thymeleaf.
   * <p>
   * GET /profile
   *
   * @param token OAuth2 authentication token providing user attributes
   * @param model model to pass attributes to the view
   * @return the profile view name
   */
  @GetMapping("/profile")
  public String restricted(
      final OAuth2AuthenticationToken token,
      final Model model) {
    model.addAttribute("email",
        token.getPrincipal().getAttribute("email"));
    model.addAttribute("name",
        token.getPrincipal().getAttribute("name"));
    return "profile";
  }
}
