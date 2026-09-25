package com.minerva.domain.entities.order;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.ArrayAttribute;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.OrderIdImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Order extends Entity<OrderId> {
    private final CustomerId customerId;
    private OrderStatus status;
    private final LocalDateTime registrationDate;
    private LocalDateTime updatedDate;
    private final Map<ProductId, OrderDetail> details = new LinkedHashMap<>();
    private final List<OrderTransition> transitions = new ArrayList<>();
    private final String deliveryAddress;
    private final String deliveryContact;
    private final String deliveryPhone;

    public Order(CustomerId customerId, List<OrderItemCreateDTO> items, UserId actorId) throws DomainException {
        this(customerId, items, actorId, null, null, null);
    }

    public Order(
            CustomerId customerId,
            List<OrderItemCreateDTO> items,
            UserId actorId,
            String deliveryAddress,
            String deliveryContact,
            String deliveryPhone
    ) throws DomainException {
        super(OrderIdImpl.generate());
        if (customerId == null) throw new DomainException("El cliente del pedido no puede estar vacio.");
        this.customerId = customerId;
        this.status = OrderStatus.PENDIENTE;
        this.registrationDate = LocalDateTime.now();
        this.updatedDate = registrationDate;
        this.deliveryAddress = normalizeNullable(deliveryAddress);
        this.deliveryContact = normalizeNullable(deliveryContact);
        this.deliveryPhone = normalizeNullable(deliveryPhone);
        addOrderItems(items);
        recordTransition(null, OrderStatus.PENDIENTE, actorId);
    }

    public Order(
            OrderId orderId,
            CustomerId customerId,
            OrderStatus status,
            LocalDateTime registrationDate,
            LocalDateTime updatedDate,
            List<OrderItemRestoreDTO> restoredItems,
            List<OrderTransition> restoredTransitions
    ) throws DomainException {
        this(orderId, customerId, status, registrationDate, updatedDate, restoredItems, restoredTransitions, null, null, null);
    }

    public Order(
            OrderId orderId,
            CustomerId customerId,
            OrderStatus status,
            LocalDateTime registrationDate,
            LocalDateTime updatedDate,
            List<OrderItemRestoreDTO> restoredItems,
            List<OrderTransition> restoredTransitions,
            String deliveryAddress,
            String deliveryContact,
            String deliveryPhone
    ) throws DomainException {
        super(orderId);
        if (customerId == null) throw new DomainException("El cliente del pedido no puede estar vacío.");
        if (status == null) throw new DomainException("El estado del pedido no puede estar vacío.");
        if (registrationDate == null) throw new DomainException("La fecha de registro del pedido no puede estar vacía.");
        if (updatedDate == null) throw new DomainException("La fecha de actualización del pedido no puede estar vacía.");
        this.customerId = customerId;
        this.status = status;
        this.registrationDate = registrationDate;
        this.updatedDate = updatedDate;
        this.deliveryAddress = normalizeNullable(deliveryAddress);
        this.deliveryContact = normalizeNullable(deliveryContact);
        this.deliveryPhone = normalizeNullable(deliveryPhone);
        restoreOrderItems(restoredItems);
        if (restoredTransitions != null) this.transitions.addAll(restoredTransitions);
    }

    public record OrderItemCreateDTO(ProductId productId, ProductQuantity quantity, Money unitPrice) {
    }

    public record OrderItemRestoreDTO(
            OrderDetailId orderDetailId,
            ProductId productId,
            ProductQuantity quantity,
            Money unitPrice
    ) {
    }

    public record OrderItemReadDTO(
            OrderId orderId,
            OrderDetailId orderDetailId,
            ProductId productId,
            ProductQuantity quantity,
            Money unitPrice
    ) {
    }

    private void addOrderItems(List<OrderItemCreateDTO> items) throws DomainException {
        if (items == null || items.isEmpty()) throw new DomainException("El pedido debe tener al menos un item.");

        for (OrderItemCreateDTO item : items) {
            if (item == null) throw new DomainException("El item del pedido no puede estar vacío.");
            if (details.containsKey(item.productId())) {
                throw new DomainException("El producto ya existe en el pedido. Modifique la cantidad en lugar de duplicarlo.");
            }
            OrderDetail detail = new OrderDetail(item.productId(), item.quantity(), item.unitPrice());
            details.put(item.productId(), detail);
        }
    }

    private void restoreOrderItems(List<OrderItemRestoreDTO> items) throws DomainException {
        if (items == null || items.isEmpty()) throw new DomainException("El pedido debe tener al menos un item.");

        for (OrderItemRestoreDTO item : items) {
            if (item == null) throw new DomainException("El item persistido del pedido no puede estar vacío.");
            if (details.containsKey(item.productId())) {
                throw new DomainException("El pedido persistido contiene productos duplicados.");
            }
            OrderDetail detail = new OrderDetail(item.orderDetailId(), item.productId(), item.quantity(), item.unitPrice());
            details.put(item.productId(), detail);
        }
    }

    public void confirm(UserId actorId) throws DomainException {
        validateConfirmation(actorId);
        changeStatus(OrderStatus.CONFIRMADO, actorId);
    }

    public void validateConfirmation(UserId actorId) throws DomainException {
        ensureTransitionAllowed(OrderStatus.CONFIRMADO, actorId, OrderStatus.PENDIENTE);
    }

    public void prepare(UserId actorId) throws DomainException {
        transitionTo(OrderStatus.EN_PREPARACION, actorId, OrderStatus.CONFIRMADO);
    }

    public void dispatch(UserId actorId) throws DomainException {
        transitionTo(OrderStatus.EN_REPARTO, actorId, OrderStatus.EN_PREPARACION);
    }

    public void deliver(UserId actorId) throws DomainException {
        transitionTo(OrderStatus.ENTREGADO, actorId, OrderStatus.EN_REPARTO);
    }

    public boolean cancel(UserId actorId) throws DomainException {
        return cancelInternal(actorId, null);
    }

    public boolean cancel(UserId actorId, String reason) throws DomainException {
        if (normalizeNullable(reason) == null) {
            throw new DomainException("El motivo de cancelacion no puede estar vacio.");
        }
        return cancelInternal(actorId, reason);
    }

    private boolean cancelInternal(UserId actorId, String reason) throws DomainException {
        ensureActor(actorId);
        if (!status.canBeCancelled()) {
            throw new DomainException("El pedido en estado " + status + " no puede cancelarse.");
        }
        boolean shouldRestoreStock = shouldRestoreStockOnCancellation();
        changeStatus(OrderStatus.CANCELADO, actorId, reason);
        return shouldRestoreStock;
    }

    public boolean shouldRestoreStockOnCancellation() {
        return status.requiresStockRestorationOnCancel();
    }

    private void transitionTo(OrderStatus targetStatus, UserId actorId, OrderStatus expectedCurrentStatus) throws DomainException {
        ensureTransitionAllowed(targetStatus, actorId, expectedCurrentStatus);
        changeStatus(targetStatus, actorId);
    }

    private void ensureTransitionAllowed(OrderStatus targetStatus, UserId actorId, OrderStatus expectedCurrentStatus)
            throws DomainException {
        ensureActor(actorId);
        if (status.isTerminal()) {
            throw new DomainException("El pedido en estado terminal " + status + " no puede cambiar de estado.");
        }
        if (status != expectedCurrentStatus) {
            throw new DomainException("Transición inválida de " + status + " a " + targetStatus + ".");
        }
    }

    private void changeStatus(OrderStatus newStatus, UserId actorId) throws DomainException {
        changeStatus(newStatus, actorId, null);
    }

    private void changeStatus(OrderStatus newStatus, UserId actorId, String cancellationReason) throws DomainException {
        OrderStatus previousStatus = status;
        status = newStatus;
        updatedDate = LocalDateTime.now();
        recordTransition(previousStatus, newStatus, actorId, cancellationReason);
    }

    private void recordTransition(OrderStatus previousStatus, OrderStatus newStatus, UserId actorId) throws DomainException {
        recordTransition(previousStatus, newStatus, actorId, null);
    }

    private void recordTransition(OrderStatus previousStatus, OrderStatus newStatus, UserId actorId, String cancellationReason) throws DomainException {
        ensureActor(actorId);
        transitions.add(new OrderTransition(previousStatus, newStatus, actorId, cancellationReason));
    }

    private void ensureActor(UserId actorId) throws DomainException {
        if (actorId == null) throw new DomainException("El actor del pedido no puede estar vacío.");
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public List<OrderItemReadDTO> getDetails() {
        List<OrderItemReadDTO> orderDetails = new ArrayList<>(details.size());
        details.forEach((productId, detail) -> orderDetails.add(new OrderItemReadDTO(
                getId(),
                detail.getId(),
                productId,
                detail.getQuantity(),
                detail.getUnitPrice()
        )));
        return List.copyOf(orderDetails);
    }

    public List<OrderTransition> getTransitions() {
        return List.copyOf(transitions);
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getDeliveryContact() {
        return deliveryContact;
    }

    public String getDeliveryPhone() {
        return deliveryPhone;
    }

    public Money calculateTotal() {
        return details.values().stream()
                .map(OrderDetail::calculateSubTotal)
                .reduce(Money.zero(), Money::add);
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute(getId()));
        attributes.add(new StringAttribute(customerId));
        attributes.add(new StringAttribute("status", status.name()));
        attributes.add(new NumericAttribute("total", calculateTotal()));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        attributes.add(new StringAttribute("updatedDate", updatedDate));
        if (deliveryAddress != null) attributes.add(new StringAttribute("deliveryAddress", deliveryAddress));
        if (deliveryContact != null) attributes.add(new StringAttribute("deliveryContact", deliveryContact));
        if (deliveryPhone != null) attributes.add(new StringAttribute("deliveryPhone", deliveryPhone));
        attributes.add(new ArrayAttribute("details", details.values().stream().map(OrderDetail::getAuditData).toList()));
        attributes.add(new ArrayAttribute("transitions", transitions.stream().map(OrderTransition::getAuditData).toList()));
        return Set.copyOf(attributes);
    }
}
