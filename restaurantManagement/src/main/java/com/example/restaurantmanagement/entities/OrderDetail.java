package com.example.restaurantmanagement.entities;

import com.example.restaurantmanagement.entities.enums.CookingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "item_id", nullable = false)
    private MenuItem menuItem;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_at_order", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceAtOrder;

    @Size(max = 100)
    @NotNull
    @Column(name = "item_name_at_order", nullable = false, length = 100)
    private String itemNameAtOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_status", nullable = false, length = 20)
    private CookingStatus cookingStatus = CookingStatus.PENDING;


}