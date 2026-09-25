package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.order.*;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.repositories.OrderRepository;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.*;
import com.minerva.infrastructure.persistence.entity.*;
import com.minerva.infrastructure.persistence.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    @PersistenceContext private EntityManager entityManager;
    private final JpaOrderRepository orders;
    private final JpaOrderDetailRepository details;
    private final JpaOrderTransitionRepository transitions;

    public OrderRepositoryAdapter(JpaOrderRepository orders, JpaOrderDetailRepository details,
                                  JpaOrderTransitionRepository transitions) {
        this.orders = orders;
        this.details = details;
        this.transitions = transitions;
    }

    @Override @Transactional
    public void save(Order order) {
        OrderEntity entity = orders.findById(order.getId().getIdValue()).orElseGet(() -> OrderEntity.builder()
                .orderId(order.getId().getIdValue())
                .customer(entityManager.getReference(CustomerEntity.class, order.getCustomerId().getIdValue()))
                .status(order.getStatus())
                .registrationDate(order.getRegistrationDate())
                .updatedDate(order.getUpdatedDate())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryContact(order.getDeliveryContact())
                .deliveryPhone(order.getDeliveryPhone())
                .build());
        entity.setStatus(order.getStatus());
        entity.setUpdatedDate(order.getUpdatedDate());
        entity.setDeliveryAddress(order.getDeliveryAddress());
        entity.setDeliveryContact(order.getDeliveryContact());
        entity.setDeliveryPhone(order.getDeliveryPhone());
        entity = orders.save(entity);

        for (Order.OrderItemReadDTO detail : order.getDetails()) {
            if (!details.existsById(detail.orderDetailId().getIdValue())) {
                details.save(new OrderDetailEntity(detail.orderDetailId().getIdValue(), entity,
                        entityManager.getReference(ProductEntity.class, detail.productId().getIdValue()),
                        detail.quantity().getValue(), detail.unitPrice().getValue()));
            }
        }
        for (OrderTransition transition : order.getTransitions()) {
            if (!transitions.existsById(transition.getId().getIdValue())) {
                transitions.save(OrderTransitionEntity.builder()
                        .orderTransitionId(transition.getId().getIdValue())
                        .order(entity)
                        .previousStatus(transition.getPreviousStatus())
                        .newStatus(transition.getNewStatus())
                        .actor(entityManager.getReference(AppUserEntity.class, transition.getActorId().getIdValue()))
                        .registrationDate(transition.getRegistrationDate())
                        .cancellationReason(transition.getCancellationReason())
                        .build());
            }
        }
    }

    @Override @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return orders.findById(orderId.getIdValue()).map(this::toDomain);
    }

    @Override @Transactional
    public Optional<Order> findByIdForUpdate(OrderId orderId) {
        return orders.findByIdForUpdate(orderId.getIdValue()).map(this::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public Optional<Order> findByIdAndCustomerId(OrderId orderId, CustomerId customerId) {
        return orders.findByOrderIdAndCustomer_CustomerId(orderId.getIdValue(), customerId.getIdValue()).map(this::toDomain);
    }

    @Override @Transactional
    public Optional<Order> findByIdAndCustomerIdForUpdate(OrderId orderId, CustomerId customerId) {
        return orders.findByIdAndCustomerIdForUpdate(orderId.getIdValue(), customerId.getIdValue()).map(this::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orders.findByCustomer_CustomerIdOrderByRegistrationDateDesc(customerId.getIdValue())
                .stream().map(this::toDomain).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<Order> findAll(OrderStatus status, CustomerId customerId, LocalDateTime from, LocalDateTime to) {
        UUID customerUuid = customerId == null ? null : customerId.getIdValue();
        Specification<OrderEntity> specification = (root, query, builder) -> builder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (customerUuid != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("customer").get("customerId"), customerUuid));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("registrationDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("registrationDate"), to));
        }
        return orders.findAll(specification, Sort.by(Sort.Direction.DESC, "registrationDate"))
                .stream().map(this::toDomain).toList();
    }

    private Order toDomain(OrderEntity entity) {
        try {
            List<Order.OrderItemRestoreDTO> restoredDetails = details
                    .findByOrder_OrderIdOrderByProduct_ProductIdAsc(entity.getOrderId()).stream()
                    .map(value -> {
                        try {
                            return new Order.OrderItemRestoreDTO(new OrderDetailIdImpl(value.getOrderDetailId()),
                                    new ProductIdImpl(value.getProduct().getProductId()),
                                    new ProductQuantity(value.getQuantity()), new Money(value.getUnitPrice()));
                        } catch (DomainException e) { throw new EntityRestoreException("Detalle de pedido inválido.", e); }
                    }).toList();
            List<OrderTransition> restoredTransitions = transitions
                    .findByOrder_OrderIdOrderByRegistrationDateAsc(entity.getOrderId()).stream()
                    .map(value -> {
                        try {
                            return new OrderTransition(new OrderTransitionIdImpl(value.getOrderTransitionId()),
                                    value.getPreviousStatus(), value.getNewStatus(),
                                    new UserName(value.getActor().getUserName()), value.getRegistrationDate(),
                                    value.getCancellationReason());
                        } catch (DomainException e) { throw new EntityRestoreException("Transición de pedido inválida.", e); }
                    }).toList();
            return new Order(new OrderIdImpl(entity.getOrderId()),
                    new CustomerIdImpl(entity.getCustomer().getCustomerId()), entity.getStatus(),
                    entity.getRegistrationDate(), entity.getUpdatedDate(), restoredDetails, restoredTransitions,
                    entity.getDeliveryAddress(), entity.getDeliveryContact(), entity.getDeliveryPhone());
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el pedido.", e);
        }
    }
}
