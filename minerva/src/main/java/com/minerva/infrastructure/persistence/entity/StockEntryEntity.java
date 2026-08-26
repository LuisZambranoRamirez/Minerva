package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntryEntity {

    @Id
    @Column(name = "stock_entry_id", nullable = false)
    private UUID stockEntryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_product",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_entry_product")
    )
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_supplier",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_entry_supplier")
    )
    private SupplierEntity supplier;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}