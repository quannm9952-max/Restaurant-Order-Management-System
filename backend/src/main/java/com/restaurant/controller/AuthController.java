package com.restaurant.controller;

import com.restaurant.security.LoginFailureHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AuthController {
  @GetMapping("/login")
  public String login(HttpServletRequest request, Model model) {
    var session = request.getSession(false);
    Object message = null;
    if (session != null) {
      message = session.getAttribute(LoginFailureHandler.ERROR_ATTRIBUTE);
      session.removeAttribute(LoginFailureHandler.ERROR_ATTRIBUTE);
      session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
    if (request.getParameterMap().containsKey("error"))
      model.addAttribute("loginError",
          message instanceof String
              ? message
              : LoginFailureHandler.INCORRECT_CREDENTIALS);
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
