package com.restaurant.entity;

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
@Table(name = "Invoice")
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "invoice_id", nullable = false)
  private Integer id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Size(max = 30)
  @NotNull
  @Column(name = "invoice_number", nullable = false, length = 30)
  private String invoiceNumber;

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @NotNull
  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @PrePersist
  public void prePersist() {
    if (this.issuedAt == null) {
      this.issuedAt = LocalDateTime.now();
    }
  }
}
