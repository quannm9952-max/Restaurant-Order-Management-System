package restaurant.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import restaurant.service.RestaurantService;

@Controller
@RequestMapping("/menu")
public class MenuController {

    private final RestaurantService service;

    public MenuController(RestaurantService service) {
        this.service = service;
    }

    /**
     * Màn hình chính UC09 – Manage Menu
     */
    @GetMapping
    public String menuManagement(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false, defaultValue = "default") String sortBy,
            @SessionAttribute("user") Map<String, Object> user,
            Model model) {

        Boolean availableFilter = null;
        if ("available".equalsIgnoreCase(availability)) {
            availableFilter = true;
        } else if ("unavailable".equalsIgnoreCase(availability)) {
            availableFilter = false;
        }

        List<Map<String, Object>> items = service.searchMenu(search, category, availableFilter, sortBy);
        Map<String, Object> stats = service.menuStats();
        List<String> categories = service.categories();

        model.addAttribute("user", user);
        model.addAttribute("items", items);
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentCategory", category != null ? category : "All");
        model.addAttribute("currentAvailability", availability != null ? availability : "all");
        model.addAttribute("currentSort", sortBy);

        return "menu";
    }

    /**
     * API JSON lấy chi tiết 1 món để nạp vào Modal Edit / View
     */
    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<?> getMenuItemJson(@PathVariable long id) {
        try {
            Map<String, Object> item = service.getMenuItem(id);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Thêm món mới
     */
    @PostMapping
    public String addMenuItem(
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam(required = false) String customCategory,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttrs) {
        try {
            String selectedCategory = (category != null && !category.isBlank()) ? category : customCategory;
            if ("__new__".equals(category) && customCategory != null && !customCategory.isBlank()) {
                selectedCategory = customCategory;
            }
            service.addMenu(name, selectedCategory, description, price, imageUrl);
            redirectAttrs.addFlashAttribute("success", "Thêm món ăn mới '" + name.trim() + "' vào thực đơn thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Lỗi thêm món: " + e.getMessage());
        }
        return "redirect:/menu";
    }

    /**
     * Cập nhật món ăn
     */
    @PostMapping("/{id}/update")
    public String updateMenuItem(
            @PathVariable long id,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam(required = false) String customCategory,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "false") boolean available,
            RedirectAttributes redirectAttrs) {
        try {
            String selectedCategory = (category != null && !category.isBlank()) ? category : customCategory;
            if ("__new__".equals(category) && customCategory != null && !customCategory.isBlank()) {
                selectedCategory = customCategory;
            }
            service.updateMenu(id, name, selectedCategory, description, price, imageUrl, available);
            redirectAttrs.addFlashAttribute("success", "Cập nhật món ăn #" + id + " ('" + name.trim() + "') thành công!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Lỗi cập nhật món #" + id + ": " + e.getMessage());
        }
        return "redirect:/menu";
    }

    /**
     * Bật/Tắt trạng thái kinh doanh (Stop Selling / Available)
     * Đảm bảo không xóa cứng dữ liệu để bảo toàn lịch sử giao dịch.
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable long id,
            @RequestParam boolean available,
            RedirectAttributes redirectAttrs) {
        try {
            service.toggleMenuAvailability(id, available);
            String msg = available 
                ? "Đã mở bán lại món ăn #" + id + " thành công. Waiter có thể chọn món này trong đơn mới."
                : "Đã ngừng kinh doanh món ăn #" + id + ". Món sẽ không xuất hiện trong danh sách gọi món mới.";
            redirectAttrs.addFlashAttribute("success", msg);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Lỗi thay đổi trạng thái món #" + id + ": " + e.getMessage());
        }
        return "redirect:/menu";
    }
}

