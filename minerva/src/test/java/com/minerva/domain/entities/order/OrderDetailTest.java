package com.minerva.domain.entities.order;

import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderDetailTest {

    @Test
    void calculateSubTotalNormalizesToMoneyScaleUsingHalfUp() throws DomainException {
        OrderDetail detail = new OrderDetail(
                ProductIdImpl.generate(),
                new ProductQuantity(new BigDecimal("1.005")),
                new Money(new BigDecimal("1.00"))
        );

        Money subtotal = detail.calculateSubTotal();

        assertEquals(new BigDecimal("1.01"), subtotal.getValue());
        assertEquals(Money.MAX_DECIMALS, subtotal.getValue().scale());
    }
}
