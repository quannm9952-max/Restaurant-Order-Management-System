package restaurant.controller;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import restaurant.service.RestaurantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final RestaurantService service;

    public AuthController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {
        return session.getAttribute("user") == null ? "login" : "redirect:/dashboard";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        Map<String, Object> user = service.login(username, password);
        if (user == null) {
            model.addAttribute("error", "Invalid username or password, or account is deactivated.");
            return "login";
        }
        session.setAttribute("user", user);
        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
