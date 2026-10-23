package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.ProductReturnReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    public ProductReturnEntity(UUID productReturnId, SaleDetailEntity saleDetail, BigDecimal quantity,
                               ProductReturnReason reason, LocalDateTime registrationDate) {
        this(productReturnId, saleDetail, null, quantity, reason, registrationDate);
    }

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_note_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_return_credit_note"))
    private CreditNoteEntity creditNote;

    @Column(name = "quantity", precision = 10, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason", nullable = false)
    private ProductReturnReason reason;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
