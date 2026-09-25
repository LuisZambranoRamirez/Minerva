package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_movement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovementEntity {
    @Id @Column(name = "inventory_movement_id", nullable = false)
    private UUID inventoryMovementId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_product", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_movement_product"))
    private ProductEntity product;
    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;
    @Column(name = "stock_before", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockBefore;
    @Column(name = "stock_after", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockAfter;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "movement_type", nullable = false)
    private InventoryMovementType movementType;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "movement_source", nullable = false)
    private InventoryMovementSource movementSource;
    @Column(name = "source_id")
    private UUID sourceId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_name", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_movement_actor"))
    private AppUserEntity actor;
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
