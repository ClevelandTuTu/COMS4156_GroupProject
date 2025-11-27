package com.project.airhotel.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.project.airhotel.user.service.AuthUserService;

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
            .requestMatchers("/", "/index", "/logout-success",
                "/oauth2/**", "/login/oauth2/**",
                "/api/auth/**", "/error").permitAll()
            .anyRequest().authenticated()
        )
        // Automatically creates the Google Login URLS
        // Google OAuth credentials in application.properties
        // redirect to Google's Auth Server /oauth2/authorization/google
        // Google's callback URL is /login/oauth2/code/google
        // Spring ignores the redirect URL that's set on Google Cloud
        .oauth2Login(oauth2 -> oauth2
            .successHandler(this::oauthSuccessHandler)
        )

        .logout(logout -> logout
            .logoutRequestMatcher(
                new org.springframework.security.web.util.matcher.RegexRequestMatcher("^/logout$",
                    "GET")
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

  private void oauthSuccessHandler(
      final jakarta.servlet.http.HttpServletRequest req,
      final jakarta.servlet.http.HttpServletResponse res,
      final Authentication authentication
  ) throws IOException {

    OAuth2User principal = (OAuth2User) authentication.getPrincipal();
    String email = principal.getAttribute("email");
    String name  = principal.getAttribute("name");

    // create/find local user

    Long userId = authUserService.findOrCreateByEmail(email, name);

    // ensure session exists and store user id
    var session = req.getSession(true);
    session.setAttribute(SESSION_USER_ID, userId);

    // store jsession cookie
    String jsessionId = session.getId();

    // return JSON response instead of redirecting
    res.setStatus(HttpStatus.OK.value());
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);

    String json = """
      {
        "user": {
          "id": %d,
          "email": "%s",
          "name": "%s"
        }
      },
      "session": {
          "jsessionId": "%s"
      }
      """.formatted(userId, escape(email), escape(name), escape(jsessionId));

    res.getWriter().write(json);
  }

  private String escape(final String s) {
    return s == null ? "" : s.replace("\"", "\\\"");
  }
}