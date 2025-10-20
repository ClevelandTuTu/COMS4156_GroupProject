package com.project.airhotel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.project.airhotel.service.auth.AuthUserService;

@Configuration
@Profile("!dev")
public class SpringConfig {

  public static final String SESSION_USER_ID = "USER_ID";

  private final AuthUserService authUserService;

  public SpringConfig(AuthUserService authUserService) {
    this.authUserService = authUserService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    // ignore CSRF check for /manager/** and /reservations/**
    RequestMatcher ignoreCsrf = (HttpServletRequest r) -> {
      String path = r.getRequestURI();
      return path.startsWith("/manager/")
          || path.equals("/reservations")
          || path.startsWith("/reservations/");
    };

    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers(ignoreCsrf))
        .authorizeHttpRequests(auth -> auth
            // Explicitly release the endpoint used for OAuth2 login to avoid error after login
            .requestMatchers("/", "/login", "/logout-success",
                "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .successHandler((req, res, authentication) -> {
              // 1) get email and name from OAuth2User
              OAuth2User principal = (OAuth2User) authentication.getPrincipal();
              String email = principal.getAttribute("email");
              String name  = principal.getAttribute("name");
              // 2) upsert user and get userId
              Long userId = authUserService.findOrCreateByEmail(email, name);
              // 3) store in Session for usage in the future
              req.getSession(true).setAttribute(SESSION_USER_ID, userId);
              // 4) redirect
              res.sendRedirect("/profile");
            })
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/logout-success")
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID")
        )
        .build();
  }
}
