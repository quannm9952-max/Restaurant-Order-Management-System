package com.example.restaurantmanagement.controllers;

import com.example.restaurantmanagement.dto.AddItemForm;
import com.example.restaurantmanagement.dto.CancelOrderForm;
import com.example.restaurantmanagement.dto.CreateOrderForm;
import com.example.restaurantmanagement.dto.OrderLineForm;
import com.example.restaurantmanagement.dto.UpdateQuantityForm;
import com.example.restaurantmanagement.entities.MenuItem;
import com.example.restaurantmanagement.entities.Order;
import com.example.restaurantmanagement.entities.OrderDetail;
import com.example.restaurantmanagement.exeptions.BusinessException;
import com.example.restaurantmanagement.exeptions.NotFoundException;
import com.example.restaurantmanagement.services.order.IOrderService;
import com.example.restaurantmanagement.services.order.OrderLine;
import jakarta.validation.Valid;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing orders (UC05).
 * // TODO(auth): chỉ WAITER - chỉ WAITER được tạo/sửa/hủy đơn.
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * List all OPEN orders.
     */
    @GetMapping
    public String listOrders(Model model) {
        List<Order> orders = orderService.getOpenOrders();
        model.addAttribute("orders", orders);
        return "orders/list";
    }

    /**
     * Show the create order form.
     */
    @GetMapping("/new")
    public String showCreateForm(@RequestParam("tableId") Integer tableId, Model model) {
        CreateOrderForm form = new CreateOrderForm();
        form.setTableId(tableId);
        form.setLines(new ArrayList<>());
        // Add one empty line by default
        form.getLines().add(new OrderLineForm());

        List<MenuItem> availableItems = orderService.getAvailableMenuItems();
        model.addAttribute("form", form);
        model.addAttribute("availableItems", availableItems);
        return "orders/new";
    }

    /**
     * Create a new order (POST).
     */
    @PostMapping
    public String createOrder(
            @Valid @ModelAttribute("form") CreateOrderForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        // Handle validation errors
        if (bindingResult.hasErrors()) {
            // For validation errors, redirect back with messages
            String errors = bindingResult.getFieldErrors().stream()
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
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đơn phải có ít nhất 1 dòng món hợp lệ.");
            return "redirect:/orders/new?tableId=" + form.getTableId();
        }

        try {
            // Convert form lines to service OrderLine
            List<OrderLine> lines = new ArrayList<>();
            for (OrderLineForm lineForm : form.getLines()) {
                if (lineForm.getItemId() != null && lineForm.getQuantity() != null && lineForm.getQuantity() >= 1) {
                    lines.add(new OrderLine(lineForm.getItemId(), lineForm.getQuantity()));
                }
            }

            Order order = orderService.createOrder(form.getTableId(), form.getNote(), lines);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo đơn #"+ order.getId() + " thành công!");
            return "redirect:/orders/" + order.getId();
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            List<MenuItem> availableItems = orderService.getAvailableMenuItems();
            redirectAttributes.addFlashAttribute("availableItems", availableItems);
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/orders/new?tableId=" + form.getTableId();
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tables";
        }
    }

    /**
     * Show order detail.
     */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model) {
        try {
            Order order = orderService.getOrderWithDetails(id);
            model.addAttribute("order", order);
            model.addAttribute("availableItems", orderService.getAvailableMenuItems());
            return "orders/detail";
        } catch (NotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    /**
     * Add items to an existing OPEN order (POST).
     */
    @PostMapping("/{id}/items")
    public String addItemToOrder(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("addForm") AddItemForm addForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
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
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Phải có ít nhất 1 dòng món hợp lệ để thêm.");
            return "redirect:/orders/" + id;
        }

        try {
            List<OrderLine> lines = new ArrayList<>();
            for (OrderLineForm lineForm : addForm.getLines()) {
                if (lineForm.getItemId() != null && lineForm.getQuantity() != null && lineForm.getQuantity() >= 1) {
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

    /**
     * Reduce quantity of an order detail line (POST).
     */
    @PostMapping("/{id}/items/{detailId}/quantity")
    public String reduceItemQuantity(
            @PathVariable("id") Integer id,
            @PathVariable("detailId") Integer detailId,
            @Valid @ModelAttribute("qtyForm") UpdateQuantityForm qtyForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
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

    /**
     * Cancel an order detail line (POST).
     */
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

    /**
     * Cancel the entire order (POST).
     */
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
