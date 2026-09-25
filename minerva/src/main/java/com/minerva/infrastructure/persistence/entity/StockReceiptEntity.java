package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_receipt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReceiptEntity {

    @Id
    @Column(name = "stock_receipt_id", nullable = false)
    private UUID stockReceiptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_supplier",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_receipt_supplier")
    )
    private SupplierEntity supplier;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
