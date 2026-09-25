package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.entities.order.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_transition")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderTransitionEntity {
    @Id @Column(name = "order_transition_id", nullable = false)
    private UUID orderTransitionId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_order", nullable = false, foreignKey = @ForeignKey(name = "fk_order_transition_order"))
    private OrderEntity order;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "previous_status")
    private OrderStatus previousStatus;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "new_status", nullable = false)
    private OrderStatus newStatus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_name", nullable = false, foreignKey = @ForeignKey(name = "fk_order_transition_actor"))
    private AppUserEntity actor;
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;
}
