package com.restaurant.auth.controller;

import com.restaurant.auth.service.EmployeeService;
import com.restaurant.entities.Account;
import com.restaurant.entities.enums.Role;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

  private final EmployeeService employeeService;

  public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("accounts", employeeService.listAccounts());
    return "employees/list";
  }

  @GetMapping("/new")
  public String createForm(Model model) {
    Map<String, Object> form = new HashMap<>();
    form.put("active", true);
    form.put("role", "WAITER");
    return renderForm(model, form, false);
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Integer id, Model model) {
    Account account = employeeService.getAccount(id);
    Map<String, Object> form = new HashMap<>();
    form.put("id", account.getAccountId());
    form.put("fullName", account.getEmployee().getFullName());
    form.put("position", account.getEmployee().getPosition());
    form.put("username", account.getUsername());
    form.put("role", account.getRole().name());
    form.put("active", account.isActive());
    return renderForm(model, form, true);
  }

  @PostMapping("/save")
  public String save(@RequestParam(required = false) Integer id,
      @RequestParam(required = false) String fullName,
      @RequestParam(required = false) String position,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String confirmPassword,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String active, Model model) {

    Map<String, Object> form = new HashMap<>();
    form.put("id", id);
    form.put("fullName", fullName);
    form.put("position", position);
    form.put("username", username);
    form.put("role", role);
    boolean activeState = active != null && !active.isBlank();
    form.put("active", activeState);

    Role parsedRole;
    try {
      parsedRole = Role.valueOf(role == null ? "" : role);
    } catch (IllegalArgumentException ex) {
      model.addAttribute("error", "Please choose a valid role.");
      return renderForm(model, form, id != null);
    }

    try {
      if (confirmPassword != null && !confirmPassword.equals(password)) {
        throw new IllegalArgumentException("Passwords do not match.");
      }
      employeeService.saveAccount(id, fullName, position, username, password,
          parsedRole, activeState);
    } catch (IllegalArgumentException ex) {
      model.addAttribute("error", ex.getMessage());
      return renderForm(model, form, id != null);
    }

    return "redirect:/employees?saved";
  }

  private String renderForm(Model model, Map<String, Object> form,
      boolean editing) {
    for (String key : new String[]{"id", "fullName", "position", "username"})
      form.putIfAbsent(key, null);
    model.addAttribute("accounts", employeeService.listAccounts());
    model.addAttribute("form", form);
    model.addAttribute("roles", Role.values());
    model.addAttribute("editing", editing);
    return "employees/form";
  }

  @PostMapping("/{id}/role")
  public String role(@PathVariable Integer id, @RequestParam String role,
      RedirectAttributes flash) {
    try {
      employeeService.assignRole(id, Role.valueOf(role));
      flash.addFlashAttribute("successMessage", "Employee role updated.");
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/employees";
  }

  @PostMapping("/{id}/lock")
  public String lock(@PathVariable Integer id, RedirectAttributes flash) {
    try {
      employeeService.lockAccount(id);
      flash.addFlashAttribute("successMessage", "Employee account locked.");
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("errorMessage", ex.getMessage());
    }
    return "redirect:/employees";
  }
}
