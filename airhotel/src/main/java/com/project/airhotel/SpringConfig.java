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

/**
 * Production-like security configuration active when the {@code dev} profile is
 * not enabled.
 * <p>
 * This configuration:
 * <p>
 * - Requires authentication for all endpoints except a small allowlist such as
 * the home page, login endpoints, OAuth2 callbacks and error page.
 * <p>
 * - Keeps CSRF enabled by default, but ignores CSRF checks for specific JSON
 * API endpoints under {@code /manager/**} and {@code /reservations/**}.
 * <p>
 * - Configures OAuth2 login with a custom success handler that persists the
 * internal user id in the HTTP session for downstream use.
 */
@Configuration
@Profile("!dev")
public class SpringConfig {

  /**
   * Session attribute key used to store the authenticated internal user id.
   */
  public static final String SESSION_USER_ID = "USER_ID";

  /**
   * Service used to get user's email.
   */
  private final AuthUserService authUserService;

  /**
   * Creates the security configuration with dependencies.
   *
   * @param authUserServ service used to resolve or create a local user record
   *                     from the OAuth2 principal
   */
  public SpringConfig(final AuthUserService authUserServ) {
    this.authUserService = authUserServ;
  }

  /**
   * Builds the primary {@link SecurityFilterChain} for non-development
   * profiles.
   * <p>
   *
   * @param http the {@link HttpSecurity} builder to configure
   * @return a configured {@link SecurityFilterChain}
   * @throws Exception if Spring Security configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      final HttpSecurity http) throws Exception {

    // ignore CSRF check for /manager/** and /reservations/**
    final RequestMatcher ignoreCsrf = (final HttpServletRequest r) -> {
      final String path = r.getRequestURI();
      return path.startsWith("/manager/")
          || path.equals("/reservations")
          || path.startsWith("/reservations/");
    };

    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers(
            ignoreCsrf))
        .authorizeHttpRequests(auth -> auth
            // Explicitly release the endpoint used for OAuth2 login to avoid
            // error after login
            .requestMatchers("/", "/login", "/logout-success",
                "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .successHandler((req, res, authentication) -> {
              // 1) get email and name from OAuth2User
              final OAuth2User principal =
                  (OAuth2User) authentication.getPrincipal();
              final String email = principal.getAttribute("email");
              final String name = principal.getAttribute("name");
              // 2) upsert user and get userId
              final Long userId = authUserService.findOrCreateByEmail(email,
                  name);
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
