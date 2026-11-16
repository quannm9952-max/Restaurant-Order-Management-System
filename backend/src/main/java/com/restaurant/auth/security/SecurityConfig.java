package com.restaurant.auth.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/login",
                        "/error",
                        "/css/**",
                        "/orders/css/**",
                        "/orders/js/**",
                        "/js/**",
                        "/images/**",
                        "/fonts/**")
                    .permitAll()
                    .requestMatchers("/kitchen/**", "/serving/**")
                    .denyAll()
                    .requestMatchers("/employees/**", "/menu", "/menu/**")
                    .hasRole("MANAGER")
                    .requestMatchers(
                        HttpMethod.GET, "/manager/**", "/history", "/reports", "/monitor")
                    .hasRole("MANAGER")
                    .requestMatchers("/manager/**")
                    .denyAll()
                    .requestMatchers(HttpMethod.GET, "/payments", "/payments/**", "/invoices/**")
                    .hasAnyRole("CASHIER", "MANAGER")
                    .requestMatchers(HttpMethod.POST, "/payments/**")
                    .hasRole("CASHIER")
                    .requestMatchers(
                        HttpMethod.GET, "/orders", "/orders/**", "/tables", "/tables/**")
                    .hasAnyRole("WAITER", "MANAGER")
                    .requestMatchers(HttpMethod.POST, "/orders/**")
                    .hasRole("WAITER")
                    .requestMatchers(HttpMethod.POST, "/tables/**")
                    .hasAnyRole("WAITER", "MANAGER")
                    .requestMatchers("/", "/home", "/dashboard", "/access-denied", "/logout")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/home", true).permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
        .exceptionHandling(errors -> errors.accessDeniedPage("/access-denied"))
        .build();
  }
}
