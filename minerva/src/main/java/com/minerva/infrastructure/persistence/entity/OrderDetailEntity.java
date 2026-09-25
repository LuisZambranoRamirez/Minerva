package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_detail", uniqueConstraints = @UniqueConstraint(name = "uk_order_detail_order_product", columnNames = {"id_order", "id_product"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetailEntity {
    @Id @Column(name = "order_detail_id", nullable = false)
    private UUID orderDetailId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_order", nullable = false, foreignKey = @ForeignKey(name = "fk_order_detail_order"))
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_product", nullable = false, foreignKey = @ForeignKey(name = "fk_order_detail_product"))
    private ProductEntity product;
    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
