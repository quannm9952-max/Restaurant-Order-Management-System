package com.example.restaurantmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TẠM THỜI - xóa file này khi module Auth của TV1 được merge.
 * 
 * Dev security configuration for local development.
 * Allows all requests without authentication. Keeps CSRF default (Thymeleaf forms will auto-attach token).
 */
@Configuration
@Profile("local")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        // CSRF is kept with default settings (enabled) so Thymeleaf POST forms work correctly
        return http.build();
    }
}
