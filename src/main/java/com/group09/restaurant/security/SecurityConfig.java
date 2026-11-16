package com.group09.restaurant.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may open which page.
 *
 * HOW OTHER MODULES PROTECT THEIR PAGES (TV2-TV5):
 * add one line in the authorizeHttpRequests block below, for example
 *     .requestMatchers("/tables/**").hasAnyRole("WAITER", "MANAGER")
 *     .requestMatchers("/kitchen/**").hasRole("CHEF")
 *     .requestMatchers("/payments/**").hasRole("CASHIER")
 * Role names: WAITER, CHEF, CASHIER, MANAGER (write them WITHOUT the ROLE_ prefix).
 *
 * To get the logged-in user inside a controller, add a parameter:
 *     public String myPage(Authentication authentication) { authentication.getName(); }
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/employees/**").hasRole("MANAGER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );
        return http.build();
    }
}
