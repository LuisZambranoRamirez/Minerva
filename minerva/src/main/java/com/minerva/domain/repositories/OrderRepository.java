package com.minerva.domain.repositories;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.order.Order;
import com.minerva.domain.entities.order.OrderId;
import com.minerva.domain.entities.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
    Optional<Order> findByIdForUpdate(OrderId orderId);
    Optional<Order> findByIdAndCustomerId(OrderId orderId, CustomerId customerId);
    Optional<Order> findByIdAndCustomerIdForUpdate(OrderId orderId, CustomerId customerId);
    List<Order> findByCustomerId(CustomerId customerId);
    List<Order> findAll(OrderStatus status, CustomerId customerId, LocalDateTime from, LocalDateTime to);
}