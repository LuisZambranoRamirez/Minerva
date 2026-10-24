package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductImagePathTest {
    @Test
    void acceptsOnlyCanonicalProductWebpKeys() throws Exception {
        assertEquals("products/arroz-extra.webp", new ProductImagePath("products/arroz-extra.webp").getValue());
    }

    @Test
    void rejectsExternalTraversalAndNonWebpValues() {
        for (String value : new String[]{"https://example.test/a.webp", "//evil/a.webp", "products/../a.webp",
                "products\\a.webp", "products/a.webp?x=1", "products/a.webp#x", "products/a.png",
                "products/a\u0000.webp", "products/Á.webp", "products/" + "a".repeat(120) + ".webp"}) {
            assertThrows(DomainException.class, () -> new ProductImagePath(value), value);
        }
    }

    @Test
    void nullIsKeptOutsideTheValueObject() {
        assertThrows(DomainException.class, () -> new ProductImagePath(null));
    }
}
