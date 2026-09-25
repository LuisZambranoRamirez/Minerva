package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.entities.order.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_order")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEntity {
    @Id @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_customer", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_order_customer"))
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "delivery_contact", length = 150)
    private String deliveryContact;

    @Column(name = "delivery_phone", length = 20)
    private String deliveryPhone;
}
