package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.Modifier;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.valueObject.Money;

import java.util.Optional;

public interface ProductSale {
    ProductId getId();
    Money getCost();
    Money getPrice();
    Optional<Money> getModifierPrice(Modifier modifier);
}
