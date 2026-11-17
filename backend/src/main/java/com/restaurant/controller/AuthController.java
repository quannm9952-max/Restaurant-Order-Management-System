package com.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AuthController {
  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping({"/", "/home", "/dashboard"})
  public String home() {
    return "home";
  }

  @RequestMapping("/access-denied")
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String denied() {
    return "access-denied";
  }

  @GetMapping("/reports")
  public String reports() {
    return "redirect:/manager/reports";
  }

  @GetMapping("/monitor")
  public String monitor() {
    return "redirect:/manager/orders";
  }
}
