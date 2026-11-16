package com.restaurant.ordermanagement.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Protects only member 5 routes. TV1 retains ownership of the application's login/account flows. */
@Configuration(proxyBeanMethods = false)
@Profile("!tv5-demo")
public class ReportingSecurityConfiguration {
    @Bean
    @Order(50)
    SecurityFilterChain reportingSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/manager/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/manager/**").hasRole("MANAGER")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults()).build();
    }
}
