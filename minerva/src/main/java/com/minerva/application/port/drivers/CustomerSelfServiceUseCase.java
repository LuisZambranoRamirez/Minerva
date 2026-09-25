package com.minerva.application.port.drivers;

import com.minerva.domain.entities.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerSelfServiceUseCase {
    enum CatalogAvailability {
        DISPONIBLE,
        AGOTADO
    }

    record CatalogItem(
            String productId,
            String sku,
            String productName,
            String category,
            String saleType,
            BigDecimal price,
            CatalogAvailability availability
    ) {}

    record CheckoutItemCommand(String productId, BigDecimal quantity) {}

    record CheckoutCommand(
            List<CheckoutItemCommand> items,
            String deliveryAddress,
            String deliveryContact,
            String deliveryPhone
    ) {}

    record OrderDetailView(
            String orderDetailId,
            String productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}

    record OrderView(
            String orderId,
            OrderStatus status,
            LocalDateTime registrationDate,
            LocalDateTime updatedDate,
            BigDecimal total,
            String deliveryAddress,
            String deliveryContact,
            String deliveryPhone,
            List<OrderDetailView> details
    ) {}

    record OrderTransitionView(
            String orderTransitionId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            String actorId,
            LocalDateTime registrationDate,
            String cancellationReason
    ) {}

    List<CatalogItem> listCatalog();
    CatalogItem getCatalogItem(String productId);
    OrderView checkout(CheckoutCommand command);
    List<OrderView> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to);
    OrderView getOrder(String orderId);
    List<OrderTransitionView> listOrderTransitions(String orderId);
    OrderView cancelOrder(String orderId, String reason);
}
