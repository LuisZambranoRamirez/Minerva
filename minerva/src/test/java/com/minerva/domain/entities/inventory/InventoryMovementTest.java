package com.minerva.domain.entities.inventory;

import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMovementTest {

    @Test
    void positiveMovementIsEntradaAndBeforeAfterIncreaseByQuantity() throws DomainException {
        InventoryMovement movement = new InventoryMovement(
                productId(),
                new BigDecimal("5"),
                quantity("10"),
                quantity("15"),
                InventoryMovementSource.STOCK_ENTRY,
                UUID.randomUUID(),
                actor()
        );

        assertEquals(InventoryMovementType.ENTRADA, movement.getType());
        assertEquals(new BigDecimal("5"), movement.getQuantity());
        assertEquals(movement.getQuantity(), movement.getStockAfter().getValue().subtract(movement.getStockBefore().getValue()));
    }

    @Test
    void negativeMovementIsSalidaAndBeforeAfterDecreaseByQuantityMagnitude() throws DomainException {
        InventoryMovement movement = new InventoryMovement(
                productId(),
                new BigDecimal("-3"),
                quantity("10"),
                quantity("7"),
                InventoryMovementSource.ORDER_CONFIRMATION,
                UUID.randomUUID(),
                actor()
        );

        assertEquals(InventoryMovementType.SALIDA, movement.getType());
        assertEquals(new BigDecimal("-3"), movement.getQuantity());
        assertEquals(movement.getQuantity(), movement.getStockAfter().getValue().subtract(movement.getStockBefore().getValue()));
    }

    @Test
    void rejectsZeroAndExplicitTypeSignMismatch() throws DomainException {
        assertThrows(DomainException.class, () -> new InventoryMovement(
                productId(), BigDecimal.ZERO, quantity("1"), quantity("1"),
                InventoryMovementSource.STOCK_ENTRY, UUID.randomUUID(), actor()
        ));

        assertThrows(DomainException.class, () -> new InventoryMovement(
                com.minerva.domain.valueObject.id.InventoryMovementIdImpl.generate(),
                productId(),
                new BigDecimal("2"),
                quantity("1"),
                quantity("3"),
                InventoryMovementType.SALIDA,
                InventoryMovementSource.STOCK_ENTRY,
                UUID.randomUUID(),
                actor(),
                java.time.LocalDateTime.now()
        ));
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
}
