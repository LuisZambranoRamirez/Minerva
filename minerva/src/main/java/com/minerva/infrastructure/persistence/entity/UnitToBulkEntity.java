package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unit_to_bulk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitToBulkEntity {

    @EmbeddedId
    private UnitToBulkId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bulkProductId")
    @JoinColumn(
            name = "bulk_product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bulk_product")
    )
    private ProductEntity bulkProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("unitProductId")
    @JoinColumn(
            name = "unit_product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_unit_product")
    )
    private ProductEntity unitProduct;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UnitToBulkId implements Serializable {

        @Column(name = "bulk_product_id")
        private UUID bulkProductId;

        @Column(name = "unit_product_id")
        private UUID unitProductId;
    }
}
