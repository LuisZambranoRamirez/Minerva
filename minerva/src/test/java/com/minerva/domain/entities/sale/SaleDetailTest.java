package com.minerva.domain.entities.sale;

import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaleDetailTest {

    @Test
    void calculateSubTotalNormalizesToMoneyScaleUsingHalfUp() throws DomainException {
        SaleDetail detail = new SaleDetail(
                new ProductQuantity(new BigDecimal("1.005")),
                new Money(new BigDecimal("1.00"))
        );

        Money subtotal = detail.calculateSubTotal();

        assertEquals(new BigDecimal("1.01"), subtotal.getValue());
        assertEquals(Money.MAX_DECIMALS, subtotal.getValue().scale());
    }
}
