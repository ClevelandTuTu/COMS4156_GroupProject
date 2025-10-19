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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.Authentication;
import com.project.airhotel.service.auth.AuthUserService;

@Configuration
public class SpringConfig {

  public static final String SESSION_USER_ID = "USER_ID";

  private final AuthUserService authUserService;

  public SpringConfig(AuthUserService authUserService) {
    this.authUserService = authUserService;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    // 忽略 /manager/** 和 /api/** 的 CSRF 校验
    RequestMatcher ignoreCsrf = (HttpServletRequest r) -> {
      String path = r.getRequestURI();
      return path.startsWith("/manager/") || path.startsWith("/api/");
    };

    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers(ignoreCsrf))
        .authorizeHttpRequests(auth -> auth
            // 把 OAuth2 登录用到的端点显式放行，避免“登录后报错”
            .requestMatchers("/", "/login", "/logout-success",
                "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .successHandler((req, res, authentication) -> {
              // 1) 从 OAuth2User 拿 email/name
              OAuth2User principal = (OAuth2User) authentication.getPrincipal();
              String email = principal.getAttribute("email");
              String name  = principal.getAttribute("name");
              // 2) upsert 用户并拿到我方 userId
              Long userId = authUserService.findOrCreateByEmail(email, name);
              // 3) 存入 Session，后续接口直接取
              req.getSession(true).setAttribute(SESSION_USER_ID, userId);
              // 4) 跳转
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
