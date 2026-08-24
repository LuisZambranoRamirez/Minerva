package com.minerva.domain.entities.product;

import com.minerva.domain.valueObject.ProductQuantity;

public interface StockEntryProduct {
    ProductId getProductId();
    ProductQuantity getQuantity();
}
