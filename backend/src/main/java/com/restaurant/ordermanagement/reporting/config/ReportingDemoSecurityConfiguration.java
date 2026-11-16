package com.restaurant.ordermanagement.reporting.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Temporary local demo only. No employee CRUD, shared Account entity or production login implementation. */
@Configuration(proxyBeanMethods = false)
@Profile("tv5-demo")
public class ReportingDemoSecurityConfiguration {
    @Bean
    UserDetailsService reportingDemoUsers(@Value("${TV5_DEMO_PASSWORD}") String password) {
        if (password.isBlank()) throw new IllegalArgumentException("Set TV5_DEMO_PASSWORD before starting tv5-demo.");
        var encoder = new BCryptPasswordEncoder();
        var users = List.of("MANAGER", "WAITER", "CHEF", "CASHIER").stream()
                .map(role -> User.withUsername("demo_" + role.toLowerCase(java.util.Locale.ROOT))
                        .password("{bcrypt}" + encoder.encode(password)).roles(role).build()).toList();
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    @Order(0)
    SecurityFilterChain reportingDemoSecurity(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/manager/**").hasRole("MANAGER")
                        .anyRequest().denyAll())
                .formLogin(login -> login.defaultSuccessUrl("/manager/reports", true).permitAll())
                .logout(Customizer.withDefaults()).build();
    }
}
