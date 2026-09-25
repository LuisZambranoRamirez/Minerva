package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "sku", length = 100, nullable = false, unique = true)
    private String sku;

    @Column(name = "product_name", length = 100, nullable = false, unique = true)
    private String productName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gain_strategy", nullable = false)
    private GainStrategy gainStrategy;

    @Column(name = "gain_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal gainAmount;

    @Column(name = "stock", precision = 10, scale = 3, nullable = false)
    private BigDecimal stock;

    @Column(name = "cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(name = "reorder_level", precision = 10, scale = 3)
    private BigDecimal reorderLevel;

    @Column(name = "bar_code", length = 13, unique = true)
    private String barCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sale_type", nullable = false)
    private SaleType saleType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}

