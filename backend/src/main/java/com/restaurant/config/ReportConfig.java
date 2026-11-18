package com.restaurant.config;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportConfig {
  @Bean
  Clock reportingClock(
      @Value("${reporting.zone:Asia/Ho_Chi_Minh}") String zone) {
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
      int decimals = amount.stripTrailingZeros().scale() > 0 ? 2 : 0;
      format.setMinimumFractionDigits(decimals);
      format.setMaximumFractionDigits(decimals);
      return format.format(amount);
    }
  }
}
