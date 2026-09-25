package com.minerva.domain.entities.order;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.id.OrderTransitionIdImpl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OrderTransition extends Entity<OrderTransitionId> {
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final UserId actorId;
    private final LocalDateTime registrationDate;
    private final String cancellationReason;

    public OrderTransition(OrderStatus previousStatus, OrderStatus newStatus, UserId actorId) throws DomainException {
        this(OrderTransitionIdImpl.generate(), previousStatus, newStatus, actorId, LocalDateTime.now(), null);
    }

    public OrderTransition(OrderStatus previousStatus, OrderStatus newStatus, UserId actorId, String cancellationReason) throws DomainException {
        this(OrderTransitionIdImpl.generate(), previousStatus, newStatus, actorId, LocalDateTime.now(), cancellationReason);
    }

    public OrderTransition(
            OrderTransitionId orderTransitionId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            UserId actorId,
            LocalDateTime registrationDate
    ) throws DomainException {
        this(orderTransitionId, previousStatus, newStatus, actorId, registrationDate, null);
    }

    public OrderTransition(
            OrderTransitionId orderTransitionId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            UserId actorId,
            LocalDateTime registrationDate,
            String cancellationReason
    ) throws DomainException {
        super(orderTransitionId);
        if (newStatus == null) throw new DomainException("El nuevo estado del pedido no puede estar vacío.");
        if (actorId == null) throw new DomainException("El actor de la transición no puede estar vacío.");
        if (registrationDate == null) throw new DomainException("La fecha de la transición no puede estar vacía.");
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actorId = actorId;
        this.registrationDate = registrationDate;
        this.cancellationReason = normalizeCancellationReason(newStatus, cancellationReason);
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public UserId getActorId() {
        return actorId;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    private static String normalizeCancellationReason(OrderStatus newStatus, String cancellationReason) throws DomainException {
        if (cancellationReason == null) return null;
        String trimmed = cancellationReason.trim();
        if (trimmed.isEmpty()) {
            if (newStatus == OrderStatus.CANCELADO) {
                throw new DomainException("El motivo de cancelacion no puede estar vacio.");
            }
            return null;
        }
        return trimmed;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute(getId()));
        if (previousStatus != null) attributes.add(new StringAttribute("previousStatus", previousStatus.name()));
        attributes.add(new StringAttribute("newStatus", newStatus.name()));
        attributes.add(new StringAttribute(actorId));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        if (cancellationReason != null) attributes.add(new StringAttribute("cancellationReason", cancellationReason));
        return Set.copyOf(attributes);
    }
}