package restaurant.controller;
import java.time.LocalDate;
import java.util.*;
import restaurant.service.RestaurantService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class DashboardController {
 private final RestaurantService s;
 public DashboardController(RestaurantService s){this.s=s;}
 @GetMapping("/dashboard") public String dashboard(@SessionAttribute("user") Map<String,Object> user,Model m){
  m.addAttribute("user",user);m.addAttribute("stats",s.stats());m.addAttribute("tables",s.tables());m.addAttribute("orders",s.orders());
  m.addAttribute("menu",s.menu());m.addAttribute("employees",s.employees());m.addAttribute("kitchen",s.kitchenItems());m.addAttribute("payments",s.payments());
  m.addAttribute("openOrders",s.openOrders());m.addAttribute("readyItems",s.readyItems());
  m.addAttribute("from",LocalDate.now().minusDays(6));m.addAttribute("to",LocalDate.now());m.addAttribute("revenue",s.revenue(LocalDate.now().minusDays(6),LocalDate.now()));
  return "dashboard";
 }
}

