package com.restaurant.ordermanagement.reporting.config;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class ReportingConfiguration {
  @Bean
  Clock reportingClock(@Value("${reporting.zone:Asia/Ho_Chi_Minh}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }

  @Bean(name = "money")
  MoneyFormatter moneyFormatter() {
    return new MoneyFormatter();
  }

  public static final class MoneyFormatter {
    public String format(BigDecimal amount) {
      return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    public String axis(BigDecimal amount) {
      NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
      format.setMinimumFractionDigits(0);
      format.setMaximumFractionDigits(0);
      return format.format(amount);
    }
  }
}
