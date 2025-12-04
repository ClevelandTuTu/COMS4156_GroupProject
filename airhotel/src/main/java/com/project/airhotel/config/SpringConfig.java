package com.project.airhotel.config;

import com.project.airhotel.user.service.AuthUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("!dev")
public class SpringConfig {

  public static final String SESSION_USER_ID = "USER_ID";

  private final AuthUserService authUserService;

  public SpringConfig(final AuthUserService authUserServ) {
    this.authUserService = authUserServ;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {

    final RequestMatcher ignoreCsrf = (final HttpServletRequest r) -> {
      final String path = r.getRequestURI();
      return path.startsWith("/manager/")
          || path.equals("/reservations")
          || path.startsWith("/reservations/");
    };

    return http
        // 1️⃣ Enable CORS so preflight & credentials work
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // 2️⃣ Keep your existing CSRF ignore
        .csrf(csrf -> csrf.ignoringRequestMatchers(ignoreCsrf))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index", "/logout-success",
                "/oauth2/**", "/login/oauth2/**",
                "/api/auth/**", "/error")
            .permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .successHandler(this::oauthSuccessHandler)
        )
        .logout(logout -> logout
            .logoutRequestMatcher(
                new org.springframework.security.web.util.matcher.RegexRequestMatcher("^/logout$", "GET")
            )
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((req, res, auth) -> {
              res.setStatus(200);
              res.setContentType("application/json");
              res.getWriter().write("{\"message\": \"Logged out successfully\"}");
            })
        )
        .build();
  }

  // 3️⃣ CORS setup: allow your React app to talk to the backend with cookies
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN"));
    config.setAllowCredentials(true); // critical for cookies
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  private void oauthSuccessHandler(
      final jakarta.servlet.http.HttpServletRequest req,
      final jakarta.servlet.http.HttpServletResponse res,
      final Authentication authentication
  ) throws IOException {

    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    String email = principal.getAttribute("email");
    String name = principal.getAttribute("name");

    Long userId = authUserService.findOrCreateByEmail(email, name);

    var session = req.getSession(true);
    session.setAttribute(SESSION_USER_ID, userId);

    String redirectUrl = "http://localhost:5173/";
    res.sendRedirect(redirectUrl);
  }
}