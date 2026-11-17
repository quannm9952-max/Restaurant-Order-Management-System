package com.restaurant.controller;

import com.restaurant.dto.preparation.*;
import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.service.PreparationService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PreparationController {
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
      .getLogger(PreparationController.class);
  private final PreparationService service;

  public PreparationController(PreparationService service) {
    this.service = service;
  }

  @GetMapping({"/kitchen", "/kitchen/"})
  public String kitchen(@RequestParam(defaultValue = "PENDING") String status,
      Model model) {
    model.addAttribute("queue", service.kitchen(status));
    model.addAttribute("selectedStatus", status);
    return "preparation/kitchen";
  }

  @GetMapping({"/serving", "/serving/"})
  public String serving(@RequestParam(required = false) Integer tableId,
      @RequestParam(defaultValue = "") String q, Model model) {
    model.addAttribute("queue", service.serving(tableId, q));
    model.addAttribute("selectedTableId", tableId);
    model.addAttribute("q", q.trim());
    return "preparation/serving";
  }

  @ResponseBody
  @GetMapping("/kitchen/updates")
  public ResponseEntity<PreparationQueue.Update> kitchenUpdate() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(service.kitchenUpdate());
  }

  @ResponseBody
  @GetMapping("/serving/updates")
  public ResponseEntity<PreparationQueue.Update> servingUpdate() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(service.servingUpdate());
  }

  @PostMapping("/kitchen/orders/{orderId}/items/{lineId}/start")
  public String start(@PathVariable Integer orderId,
      @PathVariable Integer lineId,
      @RequestParam(defaultValue = "PENDING") String status,
      RedirectAttributes flash) {
    return kitchenAction(orderId, lineId, status, false, flash);
  }

  @PostMapping("/kitchen/orders/{orderId}/items/{lineId}/ready")
  public String ready(@PathVariable Integer orderId,
      @PathVariable Integer lineId,
      @RequestParam(defaultValue = "PREPARING") String status,
      RedirectAttributes flash) {
    return kitchenAction(orderId, lineId, status, true, flash);
  }

  private String kitchenAction(Integer orderId, Integer lineId, String status,
      boolean ready, RedirectAttributes flash) {
    try {
      var line = ready
          ? service.markReady(orderId, lineId)
          : service.startPreparing(orderId, lineId);
      flash.addFlashAttribute("successMessage", message(line));
    } catch (BusinessException | NotFoundException ex) {
      flash.addFlashAttribute("errorMessage", ex.getMessage());
    } catch (DataAccessException | TransactionException ex) {
      LOG.error("Preparation update failed; transaction rolled back", ex);
      flash.addFlashAttribute("errorMessage",
          "Could not save the item status. No "
              + "change was committed. Refresh and try again.");
    }
    flash.addAttribute("status",
        java.util.Set.of("PENDING", "PREPARING", "READY").contains(status)
            ? status
            : "PENDING");
    return "redirect:/kitchen";
  }

  @PostMapping("/serving/orders/{orderId}/items/{lineId}/served")
  public String served(@PathVariable Integer orderId,
      @PathVariable Integer lineId,
      @RequestParam(required = false) Integer tableId,
      @RequestParam(defaultValue = "") String q, RedirectAttributes flash) {
    try {
      flash.addFlashAttribute("successMessage",
          message(service.confirmServed(orderId, lineId)));
    } catch (BusinessException | NotFoundException ex) {
      flash.addFlashAttribute("errorMessage", ex.getMessage());
    } catch (DataAccessException | TransactionException ex) {
      LOG.error("Preparation update failed; transaction rolled back", ex);
      flash.addFlashAttribute("errorMessage",
          "Could not save the serving confirmation. "
              + "No change was committed. Refresh and try" + " again.");
    }
    if (tableId != null && tableId > 0)
      flash.addAttribute("tableId", tableId);
    flash.addAttribute("q", q.length() <= 100 ? q.trim() : "");
    return "redirect:/serving";
  }

  private String message(PreparationLine line) {
    return line.quantity() + " × " + line.itemName() + " → "
        + line.statusLabel() + " · " + line.tableName() + " · ORD-"
        + line.orderId() + " · Line " + line.lineId()
        + ". Other lines are unchanged.";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String invalidId(Model model) {
    model.addAttribute("errorMessage",
        "Choose a valid numeric order, item or table ID.");
    return "error";
  }
}
