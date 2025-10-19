package com.project.airhotel;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SpringConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    return http
////        .csrf(csrf -> csrf
////            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
////        )
//        .authorizeHttpRequests(registry -> registry
//            .requestMatchers("/", "/login", "/logout-success").permitAll()
//            .anyRequest().authenticated()
//        )
//        .oauth2Login(oauth2Login -> oauth2Login
//            .loginPage("/login")
//            .successHandler((req, res, auth) -> res.sendRedirect("/profile"))
//        )
//        .logout(logout -> logout
//            .logoutUrl("/logout")
//            .logoutSuccessUrl("/logout-success")
//            .invalidateHttpSession(true)
//            .clearAuthentication(true)
//            .deleteCookies("JSESSIONID")
//        )
//        .build();
    return http
        .csrf(csrf -> csrf.disable())            // 关闭 CSRF（否则 PATCH/POST 仍会 403）
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()            // 全部放行
        )
        // 不要配置 oauth2Login()/formLogin()/httpBasic()
        .build();
  }
}
