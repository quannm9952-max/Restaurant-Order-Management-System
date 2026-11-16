package com.restaurant.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

  @GetMapping("/access-denied")
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
