package com.minerva.domain.entities.inventory;

public enum InventoryMovementSource {
    STOCK_ENTRY,
    ORDER_CONFIRMATION,
    ORDER_CANCELLATION,
    INVENTORY_LOSS,
    PRODUCT_RETURN,
    DIRECT_SALE
}