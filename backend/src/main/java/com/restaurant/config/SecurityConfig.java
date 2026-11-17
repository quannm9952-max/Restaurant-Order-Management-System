package com.restaurant.config;

import com.restaurant.security.LoginFailureHandler;
import com.restaurant.support.Usernames;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain applicationSecurity(HttpSecurity http,
      LoginFailureHandler loginFailureHandler) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/error", "/access-denied", "/css/**",
                "/orders/css/**", "/orders/js/**", "/js/**", "/images/**",
                "/fonts/**")
            .permitAll().requestMatchers("/kitchen", "/kitchen/**")
            .hasRole("CHEF").requestMatchers("/serving", "/serving/**")
            .hasRole("WAITER")
            .requestMatchers("/employees/**", "/menu", "/menu/**")
            .hasRole("MANAGER")
            .requestMatchers(HttpMethod.GET, "/manager/**", "/history",
                "/reports", "/monitor")
            .hasRole("MANAGER").requestMatchers("/manager/**").denyAll()
            .requestMatchers(HttpMethod.GET, "/payments", "/payments/**",
                "/invoices/**")
            .hasRole("CASHIER").requestMatchers(HttpMethod.POST, "/payments/**")
            .hasRole("CASHIER")
            .requestMatchers(HttpMethod.GET, "/orders", "/orders/**", "/tables",
                "/tables/**")
            .hasAnyRole("WAITER", "MANAGER")
            .requestMatchers(HttpMethod.POST, "/orders/**").hasRole("WAITER")
            .requestMatchers(HttpMethod.POST, "/tables/**")
            .hasAnyRole("WAITER", "MANAGER")
            .requestMatchers("/", "/home", "/dashboard", "/logout")
            .authenticated().anyRequest().denyAll())
        .formLogin(
            login -> login.loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("username").passwordParameter("password")
                .authenticationDetailsSource(request -> {
                  // Check the raw value as the login filter trims usernames.
                  if (!Usernames.isValid(request.getParameter("username")))
                    throw new BadCredentialsException("Invalid username.");
                  String password = request.getParameter("password");
                  if (password == null || password.isEmpty()
                      || password.getBytes(StandardCharsets.UTF_8).length > 72)
                    throw new BadCredentialsException("Invalid credentials.");
                  return new WebAuthenticationDetails(request);
                }).failureHandler(loginFailureHandler)
                .defaultSuccessUrl("/home", true).permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
        .exceptionHandling(errors -> errors.accessDeniedPage("/access-denied"))
        .build();
  }
}
