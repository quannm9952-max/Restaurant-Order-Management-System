package com.restaurant.exceptions;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Shared HTTP error boundary; internal exception details stay in server logs.
 */
@Order(100)
@ControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory
      .getLogger(GlobalExceptionHandler.class);

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
  public String business(RuntimeException ex, Model model) {
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class,
      ServletRequestBindingException.class})
  public String invalidInput(Exception ex, Model model) {
    model.addAttribute("errorMessage",
        "Check the required fields and enter valid values.");
    return "error";
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public String missing(NotFoundException ex, Model model) {
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({NoResourceFoundException.class,
      NoHandlerFoundException.class})
  public String unknownResource(Exception ex, Model model) {
    model.addAttribute("errorMessage", "The requested page was not found.");
    return "error";
  }

  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public String wrongMethod(Exception ex, Model model) {
    model.addAttribute("errorMessage",
        "This action does not support that method.");
    return "error";
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String denied() {
    return "access-denied";
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public String unexpected(Exception ex, Model model) {
    String reference = UUID.randomUUID().toString();
    LOG.error("Unhandled request failure [{}]", reference, ex);
    model.addAttribute("errorMessage",
        "Unable to complete this request. Please try again. Reference: "
            + reference);
    return "error";
  }
}
