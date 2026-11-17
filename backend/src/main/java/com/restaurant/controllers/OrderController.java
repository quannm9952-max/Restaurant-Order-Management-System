package com.restaurant.controllers;

import com.restaurant.dto.AddItemForm;
import com.restaurant.dto.CancelOrderForm;
import com.restaurant.dto.CreateOrderForm;
import com.restaurant.dto.OrderLineForm;
import com.restaurant.dto.UpdateQuantityForm;
import com.restaurant.dto.order.OrderDetailView;
import com.restaurant.dto.order.OrderSummaryView;
import com.restaurant.dto.table.MenuItemOption;
import com.restaurant.exceptions.BusinessException;
import com.restaurant.exceptions.NotFoundException;
import com.restaurant.services.order.IOrderService;
import com.restaurant.services.order.OrderLine;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for managing orders (UC05). Access: chỉ WAITER - chỉ WAITER được tạo/sửa/hủy
 * đơn.
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

  private final IOrderService orderService;

  public OrderController(IOrderService orderService) {
    this.orderService = orderService;
  }

  /** List all OPEN orders. */
  @GetMapping
  public String listOrders(Model model) {
    List<OrderSummaryView> orders = orderService.listOpenOrders();
    model.addAttribute("orders", orders);
    return "orders/list";
  }

  /** Show the create order form. */
  @GetMapping("/new")
  public String showCreateForm(@RequestParam("tableId") Integer tableId, Model model) {
    CreateOrderForm form = new CreateOrderForm();
    form.setTableId(tableId);
    form.setLines(new ArrayList<>());
    // Add one empty line by default
    form.getLines().add(new OrderLineForm());

    List<MenuItemOption> availableItems = orderService.listAvailableItems();
    model.addAttribute("form", form);
    model.addAttribute("availableItems", availableItems);
    return "orders/new";
  }

  /** Create a new order (POST). */
  @PostMapping
  public String createOrder(
      @Valid @ModelAttribute("form") CreateOrderForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {
    // Handle validation errors
    if (bindingResult.hasErrors()) {
      // For validation errors, redirect back with messages
      String errors =
          bindingResult.getFieldErrors().stream()
              .map(err -> err.getField() + ": " + err.getDefaultMessage())
              .reduce((a, b) -> a + "; " + b)
              .orElse("Dữ liệu không hợp lệ.");
      redirectAttributes.addFlashAttribute("errorMessage", errors);
      return "redirect:/orders/new?tableId=" + form.getTableId();
    }

    // Check at least one valid line
    boolean hasValidLine = false;
    for (OrderLineForm line : form.getLines()) {
      if (line.getItemId() != null && line.getQuantity() != null && line.getQuantity() >= 1) {
        hasValidLine = true;
        break;
      }
    }
    if (!hasValidLine) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Đơn phải có ít nhất 1 dòng món hợp lệ.");
      return "redirect:/orders/new?tableId=" + form.getTableId();
    }

    try {
      // Convert form lines to service OrderLine
      List<OrderLine> lines = new ArrayList<>();
      for (OrderLineForm lineForm : form.getLines()) {
        if (lineForm.getItemId() != null
            && lineForm.getQuantity() != null
            && lineForm.getQuantity() >= 1) {
          lines.add(new OrderLine(lineForm.getItemId(), lineForm.getQuantity()));
        }
      }

      com.restaurant.entities.Order order =
          orderService.createOrder(form.getTableId(), form.getNote(), lines);
      redirectAttributes.addFlashAttribute(
          "successMessage", "Tạo đơn #" + order.getId() + " thành công!");
      return "redirect:/orders/" + order.getId();
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      List<MenuItemOption> availableItems = orderService.listAvailableItems();
      redirectAttributes.addFlashAttribute("availableItems", availableItems);
      redirectAttributes.addFlashAttribute("form", form);
      return "redirect:/orders/new?tableId=" + form.getTableId();
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
      return "redirect:/tables";
    }
  }

  /** Show order detail. */
  @GetMapping("/{id}")
  public String orderDetail(@PathVariable("id") Integer id, Model model) {
    try {
      OrderDetailView order = orderService.getOrderDetail(id);
      model.addAttribute("order", order);
      model.addAttribute("availableItems", orderService.listAvailableItems());
      return "orders/detail";
    } catch (NotFoundException e) {
      model.addAttribute("errorMessage", e.getMessage());
      return "error";
    }
  }

  /** Add items to an existing OPEN order (POST). */
  @PostMapping("/{id}/items")
  public String addItemToOrder(
      @PathVariable("id") Integer id,
      @Valid @ModelAttribute("addForm") AddItemForm addForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "errorMessage",
          "Dữ liệu không hợp lệ: " + bindingResult.getFieldErrors().get(0).getDefaultMessage());
      return "redirect:/orders/" + id;
    }

    // Check at least one valid line
    boolean hasValidLine = false;
    for (OrderLineForm line : addForm.getLines()) {
      if (line.getItemId() != null && line.getQuantity() != null && line.getQuantity() >= 1) {
        hasValidLine = true;
        break;
      }
    }
    if (!hasValidLine) {
      redirectAttributes.addFlashAttribute(
          "errorMessage", "Phải có ít nhất 1 dòng món hợp lệ để thêm.");
      return "redirect:/orders/" + id;
    }

    try {
      List<OrderLine> lines = new ArrayList<>();
      for (OrderLineForm lineForm : addForm.getLines()) {
        if (lineForm.getItemId() != null
            && lineForm.getQuantity() != null
            && lineForm.getQuantity() >= 1) {
          lines.add(new OrderLine(lineForm.getItemId(), lineForm.getQuantity()));
        }
      }
      orderService.addItemToOrder(id, lines);
      redirectAttributes.addFlashAttribute("successMessage", "Thêm món thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Reduce quantity of an order detail line (POST). */
  @PostMapping("/{id}/items/{detailId}/quantity")
  public String reduceItemQuantity(
      @PathVariable("id") Integer id,
      @PathVariable("detailId") Integer detailId,
      @Valid @ModelAttribute("qtyForm") UpdateQuantityForm qtyForm,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "errorMessage",
          "Dữ liệu không hợp lệ: " + bindingResult.getFieldErrors().get(0).getDefaultMessage());
      return "redirect:/orders/" + id;
    }

    try {
      orderService.reduceItemQuantity(id, detailId, qtyForm.getNewQuantity());
      redirectAttributes.addFlashAttribute("successMessage", "Cập nhật số lượng thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Cancel an order detail line (POST). */
  @PostMapping("/{id}/items/{detailId}/cancel")
  public String cancelOrderDetail(
      @PathVariable("id") Integer id,
      @PathVariable("detailId") Integer detailId,
      RedirectAttributes redirectAttributes) {
    try {
      orderService.cancelOrderDetail(id, detailId);
      redirectAttributes.addFlashAttribute("successMessage", "Hủy dòng món thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Cancel the entire order (POST). */
  @PostMapping("/{id}/cancel")
  public String cancelOrder(
      @PathVariable("id") Integer id,
      @ModelAttribute("cancelForm") CancelOrderForm cancelForm,
      RedirectAttributes redirectAttributes) {
    try {
      orderService.cancelOrder(id, cancelForm.getReason());
      redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders";
  }
}
