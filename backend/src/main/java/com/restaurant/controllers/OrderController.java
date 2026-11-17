package com.restaurant.controllers;

import com.restaurant.entities.enums.OrderStatus;

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
import com.restaurant.services.table.ITableService;
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
 * Controller for managing orders (UC05). Access: chỉ WAITER - chỉ WAITER được
 * tạo/sửa/hủy đơn.
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

  private final IOrderService orderService;
  private final ITableService tableService;

  public OrderController(IOrderService orderService,
      ITableService tableService) {
    this.orderService = orderService;
    this.tableService = tableService;
  }

  @ModelAttribute
  public void workspace(Model model) {
    model.addAttribute("tables", tableService.listTables());
    var items = orderService.listAvailableItems();
    model.addAttribute("availableItems", items);
    model.addAttribute("categories", items.stream()
        .map(MenuItemOption::category).distinct().sorted().toList());
  }

  /** List all OPEN orders. */
  @GetMapping
  public String listOrders(
      @RequestParam(defaultValue = "OPEN") OrderStatus status, Model model) {
    model.addAttribute("selectedStatus", status.name());
    if (status != OrderStatus.OPEN) {
      model.addAttribute("orders", orderService.listOrdersByStatus(status));
      return "orders/history";
    }
    List<OrderSummaryView> orders = orderService.listOpenOrders();
    model.addAttribute("orders", orders);
    var tables = tableService.listTables();
    var selected = tables.stream()
        .filter(t -> t.canCreateOrder() || t.currentOrderId() != null)
        .findFirst();
    if (selected.isPresent()) {
      model.addAttribute("selectedTable", selected.get());
      if (selected.get().currentOrderId() != null)
        model.addAttribute("order",
            orderService.getOrderDetail(selected.get().currentOrderId()));
    }
    return "orders/workspace";
  }

  /** Show the create order form. */
  @GetMapping("/new")
  public String showCreateForm(@RequestParam("tableId") Integer tableId,
      Model model) {
    CreateOrderForm form = model.containsAttribute("form")
        ? (CreateOrderForm) model.getAttribute("form")
        : new CreateOrderForm();
    form.setTableId(tableId);
    if (form.getLines() == null)
      form.setLines(new ArrayList<>());

    List<MenuItemOption> availableItems = orderService.listAvailableItems();
    model.addAttribute("form", form);
    model.addAttribute("availableItems", availableItems);
    model.addAttribute("selectedTable", tableService.getTable(tableId));
    return "orders/workspace";
  }

  /** Create a new order (POST). */
  @PostMapping
  public String createOrder(@Valid @ModelAttribute("form") CreateOrderForm form,
      BindingResult bindingResult, RedirectAttributes redirectAttributes,
      Model model) {
    // Handle validation errors
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("form", form);
      // For validation errors, redirect back with messages
      String errors = bindingResult.getFieldErrors().stream()
          .map(err -> err.getField() + ": " + err.getDefaultMessage())
          .reduce((a, b) -> a + "; " + b).orElse("Dữ liệu không hợp lệ.");
      redirectAttributes.addFlashAttribute("errorMessage", errors);
      return "redirect:/orders/new?tableId=" + form.getTableId();
    }

    // Check at least one valid line
    boolean hasValidLine = false;
    for (OrderLineForm line : form.getLines()) {
      if (line.getItemId() != null && line.getQuantity() != null
          && line.getQuantity() >= 1) {
        hasValidLine = true;
        break;
      }
    }
    if (!hasValidLine) {
      redirectAttributes.addFlashAttribute("form", form);
      redirectAttributes.addFlashAttribute("errorMessage",
          "Đơn phải có ít nhất 1 dòng món hợp lệ.");
      return "redirect:/orders/new?tableId=" + form.getTableId();
    }

    try {
      // Convert form lines to service OrderLine
      List<OrderLine> lines = new ArrayList<>();
      for (OrderLineForm lineForm : form.getLines()) {
        if (lineForm.getItemId() != null && lineForm.getQuantity() != null
            && lineForm.getQuantity() >= 1) {
          lines
              .add(new OrderLine(lineForm.getItemId(), lineForm.getQuantity()));
        }
      }

      com.restaurant.entities.Order order = orderService
          .createOrder(form.getTableId(), form.getNote(), lines);
      redirectAttributes.addFlashAttribute("successMessage",
          "Tạo đơn #" + order.getId() + " thành công!");
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
    OrderDetailView order = orderService.getOrderDetail(id);
    model.addAttribute("order", order);
    model.addAttribute("selectedTable", tableService.getTable(order.tableId()));
    model.addAttribute("availableItems", orderService.listAvailableItems());
    return "orders/workspace";
  }

  /** Add items to an existing OPEN order (POST). */
  @PostMapping("/{id}/items")
  public String addItemToOrder(@PathVariable("id") Integer id,
      @Valid @ModelAttribute("addForm") AddItemForm addForm,
      BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "Dữ liệu không hợp lệ: "
              + bindingResult.getFieldErrors().get(0).getDefaultMessage());
      return "redirect:/orders/" + id;
    }

    // Check at least one valid line
    boolean hasValidLine = false;
    for (OrderLineForm line : addForm.getLines()) {
      if (line.getItemId() != null && line.getQuantity() != null
          && line.getQuantity() >= 1) {
        hasValidLine = true;
        break;
      }
    }
    if (!hasValidLine) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "Phải có ít nhất 1 dòng món hợp lệ để thêm.");
      return "redirect:/orders/" + id;
    }

    try {
      List<OrderLine> lines = new ArrayList<>();
      for (OrderLineForm lineForm : addForm.getLines()) {
        if (lineForm.getItemId() != null && lineForm.getQuantity() != null
            && lineForm.getQuantity() >= 1) {
          lines
              .add(new OrderLine(lineForm.getItemId(), lineForm.getQuantity()));
        }
      }
      orderService.addItemToOrder(id, lines);
      redirectAttributes.addFlashAttribute("successMessage",
          "Thêm món thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Reduce quantity of an order detail line (POST). */
  @PostMapping("/{id}/items/{detailId}/quantity")
  public String reduceItemQuantity(@PathVariable("id") Integer id,
      @PathVariable("detailId") Integer detailId,
      @Valid @ModelAttribute("qtyForm") UpdateQuantityForm qtyForm,
      BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage",
          "Dữ liệu không hợp lệ: "
              + bindingResult.getFieldErrors().get(0).getDefaultMessage());
      return "redirect:/orders/" + id;
    }

    try {
      orderService.reduceItemQuantity(id, detailId, qtyForm.getNewQuantity());
      redirectAttributes.addFlashAttribute("successMessage",
          "Cập nhật số lượng thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Cancel an order detail line (POST). */
  @PostMapping("/{id}/items/{detailId}/cancel")
  public String cancelOrderDetail(@PathVariable("id") Integer id,
      @PathVariable("detailId") Integer detailId,
      RedirectAttributes redirectAttributes) {
    try {
      orderService.cancelOrderDetail(id, detailId);
      redirectAttributes.addFlashAttribute("successMessage",
          "Hủy dòng món thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders/" + id;
  }

  /** Cancel the entire order (POST). */
  @PostMapping("/{id}/cancel")
  public String cancelOrder(@PathVariable("id") Integer id,
      @ModelAttribute("cancelForm") CancelOrderForm cancelForm,
      RedirectAttributes redirectAttributes) {
    try {
      orderService.cancelOrder(id, cancelForm.getReason());
      redirectAttributes.addFlashAttribute("successMessage",
          "Hủy đơn thành công!");
    } catch (BusinessException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    } catch (NotFoundException e) {
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/orders";
  }
}
