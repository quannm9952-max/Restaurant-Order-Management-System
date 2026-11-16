package com.restaurant.exceptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application. Catches exceptions thrown by controllers and adds
 * error messages to the model. Controllers should use RedirectAttributes for POST-redirect-GET
 * pattern with flash messages. This handler mainly serves as a safety net for uncaught exceptions.
 */
@org.springframework.core.annotation.Order(100)
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle business rule violations — adds error message to model. Controllers catching this
   * exception should redirect with flash message.
   */
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.BAD_REQUEST)
  @ExceptionHandler(BusinessException.class)
  public String handleBusinessException(BusinessException ex, Model model) {
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
  }

  /** Handle not found exceptions. */
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public String handleNotFoundException(NotFoundException ex, Model model) {
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.FORBIDDEN)
  public String denied() {
    return "access-denied";
  }

  /** Handle generic exceptions as a last resort. */
  @org.springframework.web.bind.annotation.ResponseStatus(
      org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public String handleGenericException(Exception ex, Model model) {
    model.addAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
    return "error";
  }
}
