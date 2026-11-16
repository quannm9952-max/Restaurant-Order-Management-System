package restaurant.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import restaurant.service.RestaurantService;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner createDemoAccounts(RestaurantService service) {
        return args -> service.seedDemoData();
    }
}

