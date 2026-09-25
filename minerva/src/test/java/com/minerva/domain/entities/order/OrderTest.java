package com.minerva.domain.entities.order;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void createsPendingOrderWithoutStockSideEffectsAndRecordsInitialTransition() throws DomainException {
        Order order = newPendingOrder();

        assertEquals(OrderStatus.PENDIENTE, order.getStatus());
        assertEquals(1, order.getDetails().size());
        assertEquals(new BigDecimal("2"), order.getDetails().get(0).quantity().getValue());
        assertEquals(1, order.getTransitions().size());
        assertNull(order.getTransitions().get(0).getPreviousStatus());
        assertEquals(OrderStatus.PENDIENTE, order.getTransitions().get(0).getNewStatus());
    }

    @Test
    void validWorkflowTransitionsInOrderAndAppendsHistory() throws DomainException {
        Order order = newPendingOrder();
        UserId actor = actor();

        order.confirm(actor);
        order.prepare(actor);
        order.dispatch(actor);
        order.deliver(actor);

        assertEquals(OrderStatus.ENTREGADO, order.getStatus());
        assertEquals(List.of(
                OrderStatus.PENDIENTE,
                OrderStatus.CONFIRMADO,
                OrderStatus.EN_PREPARACION,
                OrderStatus.EN_REPARTO,
                OrderStatus.ENTREGADO
        ), order.getTransitions().stream().map(OrderTransition::getNewStatus).toList());
    }

    @Test
    void rejectsIllegalTransitionAndLeavesStateUntouched() throws DomainException {
        Order order = newPendingOrder();

        DomainException exception = assertThrows(DomainException.class, () -> order.prepare(actor()));

        assertTrue(exception.getMessage().contains("Transición inválida"));
        assertEquals(OrderStatus.PENDIENTE, order.getStatus());
        assertEquals(1, order.getTransitions().size());
    }

    @Test
    void cancellationRulesDistinguishStockRestorationAndTerminalStates() throws DomainException {
        Order pending = restoredOrder(OrderStatus.PENDIENTE);
        Order confirmed = restoredOrder(OrderStatus.CONFIRMADO);
        Order preparing = restoredOrder(OrderStatus.EN_PREPARACION);
        Order dispatched = restoredOrder(OrderStatus.EN_REPARTO);
        Order delivered = restoredOrder(OrderStatus.ENTREGADO);

        assertFalse(pending.cancel(actor()));
        assertTrue(confirmed.cancel(actor()));
        assertTrue(preparing.cancel(actor()));
        assertEquals(OrderStatus.CANCELADO, pending.getStatus());
        assertEquals(OrderStatus.CANCELADO, confirmed.getStatus());
        assertEquals(OrderStatus.CANCELADO, preparing.getStatus());

        assertThrows(DomainException.class, () -> dispatched.cancel(actor()));
        assertThrows(DomainException.class, () -> delivered.cancel(actor()));
        assertThrows(DomainException.class, () -> confirmed.confirm(actor()));
    }

    private static Order newPendingOrder() throws DomainException {
        return new Order(customerId(), List.of(new Order.OrderItemCreateDTO(
                productId(), quantity("2"), money("10.00")
        )), actor());
    }

    private static Order restoredOrder(OrderStatus status) throws DomainException {
        return new Order(
                com.minerva.domain.valueObject.id.OrderIdImpl.generate(),
                customerId(),
                status,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().minusMinutes(1),
                List.of(new Order.OrderItemRestoreDTO(
                        com.minerva.domain.valueObject.id.OrderDetailIdImpl.generate(),
                        productId(),
                        quantity("2"),
                        money("10.00")
                )),
                List.of()
        );
    }

    private static CustomerId customerId() {
        return CustomerIdImpl.generate();
    }

    private static ProductId productId() {
        return ProductIdImpl.generate();
    }

    private static UserId actor() throws DomainException {
        return new UserName("tester1");
    }

    private static ProductQuantity quantity(String value) throws DomainException {
        return new ProductQuantity(new BigDecimal(value));
    }

    private static Money money(String value) throws DomainException {
        return new Money(new BigDecimal(value));
    }
}
