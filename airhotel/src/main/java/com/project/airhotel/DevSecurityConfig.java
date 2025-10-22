package com.project.airhotel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development-only security configuration.
 * <p>
 * Active when the {@code dev} Spring profile is enabled. This configuration
 * disables CSRF protection and permits all HTTP requests to simplify local
 * development and manual testing. Do not enable this profile in production.
 *
 * @author Ziyang Su
 * @version 1.0.0
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

  /**
   * Builds a permissive {@link SecurityFilterChain} for development:
   * CSRF is disabled and all requests are allowed without authentication.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return a {@link SecurityFilterChain} that permits all requests
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain devSecurityFilterChain(
      final HttpSecurity http)
      throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth
            -> auth.anyRequest().permitAll())
        .build();
  }
}
