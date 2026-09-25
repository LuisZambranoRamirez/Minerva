package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.InventoryLossReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_loss")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLossEntity {

    @Id
    @Column(name = "stock_loss_id")
    private UUID inventoryLossId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_product",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_loss_product")
    )
    private ProductEntity product;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason", nullable = false)
    private InventoryLossReason reason;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
