package com.restaurant.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
public class UploadConfig {
  public static final String ERROR_ATTRIBUTE = "menuUploadError";

  @Bean
  FilterRegistrationBean<MultipartFilter> menuMultipartFilter(
      SecurityFilterProperties security) {
    var filter = new MultipartFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request,
          HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
        if (request instanceof MultipartHttpServletRequest) {
          chain.doFilter(request, response);
          return;
        }
        try {
          super.doFilterInternal(request, response, chain);
        } catch (MultipartException ex) {
          request.getSession().setAttribute(ERROR_ATTRIBUTE,
              "Unable to upload the file. Choose a JPG or PNG image up to 5 MB.");
          response.sendRedirect(request.getContextPath() + "/menu");
        }
      }
    };
    filter.setMultipartResolverBeanName("multipartResolver");
    var registration = new FilterRegistrationBean<MultipartFilter>(filter);
    registration.setOrder(security.getOrder() - 1);
    registration.addUrlPatterns("/menu", "/menu/*");
    return registration;
  }
}
