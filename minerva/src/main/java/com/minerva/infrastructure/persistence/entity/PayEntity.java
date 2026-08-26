package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayEntity {

    @Id
    @Column(name = "pay_id")
    private UUID payId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_sale",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pay_sale")
    )
    private SaleEntity sale;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}

