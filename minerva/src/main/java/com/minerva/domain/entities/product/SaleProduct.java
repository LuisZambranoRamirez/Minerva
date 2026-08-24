package com.minerva.domain.entities.product;

import com.minerva.domain.valueObject.ProductQuantity;

import java.util.Optional;

public interface SaleProduct {
    Optional<ProductQuantity> getSoldQuantity(ProductId productId);
}
