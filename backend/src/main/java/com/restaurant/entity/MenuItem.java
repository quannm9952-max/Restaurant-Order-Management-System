package com.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Menu_item")
public class MenuItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id", nullable = false)
  private Integer id;

  @Size(max = 150)
  @NotNull
  @Column(name = "item_name", nullable = false, length = 150)
  private String itemName;

  @Size(max = 255)
  @Column(name = "description")
  private String description;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "is_available", nullable = false)
  private Boolean isAvailable = true;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private MenuCategory menuCategory;
}
