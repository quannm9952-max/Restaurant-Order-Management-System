package com.example.restaurantmanagement.exeptions;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application.
 * Catches exceptions thrown by controllers and adds error messages to the model.
 * Controllers should use RedirectAttributes for POST-redirect-GET pattern with flash messages.
 * This handler mainly serves as a safety net for uncaught exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle business rule violations — adds error message to model.
     * Controllers catching this exception should redirect with flash message.
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * Handle not found exceptions.
     */
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFoundException(NotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    /**
     * Handle generic exceptions as a last resort.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
        return "error";
    }
}
