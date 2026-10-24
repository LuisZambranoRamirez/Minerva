package com.minerva.domain.entities.product;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImagePathTest {
    @Test
    void legacyProductKeepsNullableImage() throws Exception {
        Product product = new Product("TEST-0001-DEMO", "Producto Legacy", GainStrategy.PORCENTAJE,
                new BigDecimal("20"), BigDecimal.ONE, null, SaleType.GRANEL, BigDecimal.ONE,
                ProductCategory.OTROS, BigDecimal.ONE);
        assertTrue(product.getImagePath().isEmpty());
    }

    @Test
    void productStoresValidatedAssetKey() throws Exception {
        Product product = new Product("TEST-0002-DEMO", "Producto Imagen", GainStrategy.PORCENTAJE,
                new BigDecimal("20"), BigDecimal.ONE, null, SaleType.GRANEL, BigDecimal.ONE,
                ProductCategory.OTROS, BigDecimal.ONE, "products/producto-imagen.webp");
        assertEquals("products/producto-imagen.webp", product.getImagePath().orElseThrow().getValue());
    }
}
