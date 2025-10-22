package com.project.airhotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration
    .EnableWebSecurity;

/**
 * Application entry point for AirHotel. Bootstraps the Spring Boot application
 * and enables Spring Security web support. Additional security configuration
 * should be provided via standard WebSecurityConfigurer components.
 */
@SpringBootApplication
@EnableWebSecurity
public class AirhotelApplication {

  /**
   * Starts the AirHotel application.
   *
   * @param args standard JVM command-line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(AirhotelApplication.class, args);
  }

}
