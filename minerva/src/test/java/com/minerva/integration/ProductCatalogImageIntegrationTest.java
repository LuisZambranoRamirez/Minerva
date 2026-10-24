package com.minerva.integration;

import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import com.minerva.infrastructure.persistence.entity.ProductEntity;
import com.minerva.infrastructure.persistence.repository.JpaProductRepository;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductCatalogImageIntegrationTest extends IntegrationTest {

    @Autowired JpaProductRepository products;
    @Autowired EntityManager entityManager;

    @Test
    @Transactional
    void jpaRoundTripPreservesPresentAndNullableImagePath() {
        ProductEntity withImage = entity(UUID.randomUUID(), "IMAG-RNDT-0001", "Producto con imagen",
                "7791000000001", "products/arroz-extra.webp");
        ProductEntity withoutImage = entity(UUID.randomUUID(), "NULL-RNDT-0002", "Producto sin imagen",
                "7791000000002", null);

        products.save(withImage);
        products.save(withoutImage);
        products.flush();
        entityManager.clear();

        assertEquals("products/arroz-extra.webp", products.findById(withImage.getProductId()).orElseThrow().getImagePath());
        assertNull(products.findById(withoutImage.getProductId()).orElseThrow().getImagePath());
    }

    @Test
    void httpRejectsInvalidPathAndSerializesOnlySameOriginPublicImageUrls() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        IntegrationFixtures.UserFixture customer = fixtures.approvedCustomerUser();
        String adminJwt = api.login(admin);
        String customerJwt = api.login(customer);
        UUID supplierId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO supplier (supplier_id, supplier_name, registration_date)
                VALUES (?, ?, ?)
                """, supplierId, "Proveedor imágenes", IntegrationFixtures.FIXED_TIME);

        api.post("/api/v1/products", adminJwt, registerRequest(supplierId, "../escape.webp"))
                .andExpect(status().isBadRequest());
        assertEquals(0L, count("SELECT COUNT(*) FROM product"));

        api.post("/api/v1/products", adminJwt, registerRequest(supplierId, "products/arroz-extra.webp"))
                .andExpect(status().isCreated());

        api.get("/api/v1/products", adminJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value("/assets/products/arroz-extra.webp"));

        api.get("/api/v1/customer/catalog", customerJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value("/assets/products/arroz-extra.webp"))
                .andExpect(jsonPath("$[0].cost").doesNotExist())
                .andExpect(jsonPath("$[0].supplier").doesNotExist())
                .andExpect(jsonPath("$[0].supplierId").doesNotExist());
    }

    @Test
    void httpRegistrationWithoutImagePathKeepsNullOnRoundTripAndListing() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        String adminJwt = api.login(admin);
        UUID supplierId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO supplier (supplier_id, supplier_name, registration_date)
                VALUES (?, ?, ?)
                """, supplierId, "Proveedor producto sin imagen", IntegrationFixtures.FIXED_TIME);

        Map<String, Object> request = registerRequest(supplierId, null);
        request.remove("imagePath");

        api.post("/api/v1/products", adminJwt, request)
                .andExpect(status().isCreated());

        UUID productId = jdbcTemplate.queryForObject(
                "SELECT product_id FROM product WHERE sku = 'HTTP-IMAG-0001'", UUID.class);
        api.get("/api/v1/products/" + productId, adminJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
        api.get("/api/v1/products", adminJwt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'HTTP-IMAG-0001')].imageUrl").value(hasItem(nullValue())));
    }

    private Map<String, Object> registerRequest(UUID supplierId, String imagePath) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sku", "HTTP-IMAG-0001");
        request.put("productName", "Producto HTTP Imagen");
        request.put("gainStrategy", "PORCENTAJE");
        request.put("gainAmount", new BigDecimal("20"));
        request.put("reorderLevel", new BigDecimal("2"));
        request.put("barCode", "7791000000010");
        request.put("saleType", "UNIDAD");
        request.put("productCategory", "OTROS");
        request.put("purchasedFromSupplierId", supplierId.toString());
        request.put("purchaseUnitPrice", new BigDecimal("10"));
        request.put("purchaseQuantity", new BigDecimal("5"));
        request.put("purchaseExpirationDate", LocalDateTime.now().plusMonths(1).toString());
        request.put("imagePath", imagePath);
        return request;
    }

    private ProductEntity entity(UUID id, String sku, String name, String barcode, String imagePath) {
        return new ProductEntity(id, null, sku, name, GainStrategy.PORCENTAJE, new BigDecimal("20"),
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("2"), barcode,
                SaleType.UNIDAD, ProductCategory.OTROS, IntegrationFixtures.FIXED_TIME, imagePath);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }
}
