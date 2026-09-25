package com.minerva.application.port.drivers;

import com.minerva.domain.entities.order.Order;
import com.minerva.domain.entities.order.OrderStatus;
import com.minerva.domain.entities.order.OrderTransition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderUseCase {
    record OrderItemCommand(String productId, BigDecimal quantity, BigDecimal unitPrice) {}
    record CreateOrderCommand(String customerId, List<OrderItemCommand> items) {}

    Order create(CreateOrderCommand command);
    Order findById(String orderId);
    List<Order> findAll(OrderStatus status, String customerId, LocalDateTime from, LocalDateTime to);
    List<OrderTransition> findTransitions(String orderId);
    Order confirm(String orderId);
    Order prepare(String orderId);
    Order dispatch(String orderId);
    Order deliver(String orderId);
    Order cancel(String orderId);
}
