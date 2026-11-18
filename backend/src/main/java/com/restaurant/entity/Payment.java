package com.restaurant.entity;

import com.restaurant.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Payment")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_id", nullable = false)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "payment_time", nullable = false)
  private LocalDateTime paymentDate;

  @NotNull
  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Size(max = 30)
  @Column(name = "payment_method", length = 30)
  private String method;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status = PaymentStatus.PENDING;

  @Size(max = 100)
  @Column(name = "transaction_reference", length = 100)
  private String transactionReference;

  @PrePersist
  public void prePersist() {
    if (this.paymentDate == null) {
      this.paymentDate = LocalDateTime.now();
    }
  }
}
