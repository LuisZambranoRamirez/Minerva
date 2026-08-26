package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.ProductReturnReason;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_return")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReturnEntity {

    @Id
    @Column(name = "product_return_id")
    private UUID productReturnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_sale_detail",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_return_sale_detail")
    )
    private SaleDetailEntity saleDetail;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ProductReturnReason reason;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}