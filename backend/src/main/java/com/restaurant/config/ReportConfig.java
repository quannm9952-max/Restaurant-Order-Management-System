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
      NumberFormat format = NumberFormat
          .getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
      // Retain fractional legacy amounts instead of silently rounding stored
      // money.
      format.setMinimumFractionDigits(0);
      format.setMaximumFractionDigits(2);
      return format.format(amount);
    }

    public String axis(BigDecimal amount) {
      return format(amount);
    }
  }
}
