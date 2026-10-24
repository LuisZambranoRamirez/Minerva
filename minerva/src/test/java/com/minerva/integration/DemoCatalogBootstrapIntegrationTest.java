package com.minerva.integration;

import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.SupplierRepository;
import com.minerva.infrastructure.bootstrap.DemoCatalogBootstrap;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemoCatalogBootstrapIntegrationTest extends IntegrationTest {

    @Autowired ApplicationContext context;
    @Autowired SupplierRepository suppliers;
    @Autowired ProductRepository products;
    @Autowired TransactionTemplate transactions;

    @Test
    void disabledSeedDoesNotCreateAnything() {
        assertEquals(0, context.getBeansOfType(DemoCatalogBootstrap.class).size());
        assertEquals(0L, demoProducts());
        assertEquals(0L, demoSuppliers());
    }

    @Test
    void doubleOptInSeedCreatesExactDatasetAndRerunPreservesOperationalChanges() {
        runSeed();

        assertEquals(11L, demoProducts());
        assertEquals(6L, demoSuppliers());
        assertEquals(11L, count("SELECT COUNT(*) FROM stock_receipt sr JOIN stock_entry se ON se.id_stock_receipt = sr.stock_receipt_id JOIN product p ON p.product_id = se.id_product WHERE p.sku LIKE '%-DEMO'"));
        assertEquals(11L, count("SELECT COUNT(*) FROM stock_entry se JOIN product p ON p.product_id = se.id_product WHERE p.sku LIKE '%-DEMO'"));
        assertExactCatalogRows();

        jdbcTemplate.update("UPDATE product SET stock = ?, cost = ? WHERE sku = 'ARRO-0001-DEMO'",
                new BigDecimal("7"), new BigDecimal("99.99"));
        runSeed();

        assertEquals(11L, demoProducts());
        assertEquals(6L, demoSuppliers());
        assertEquals(new BigDecimal("7.000"), decimal("SELECT stock FROM product WHERE sku = 'ARRO-0001-DEMO'"));
        assertEquals(new BigDecimal("99.99"), decimal("SELECT cost FROM product WHERE sku = 'ARRO-0001-DEMO'"));
        assertEquals(11L, count("SELECT COUNT(*) FROM stock_entry se JOIN product p ON p.product_id = se.id_product WHERE p.sku LIKE '%-DEMO'"));
    }

    private void assertExactCatalogRows() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.sku, p.bar_code, p.sale_type::text AS sale_type, p.category::text AS category,
                       p.cost, p.gain_amount, p.stock, p.reorder_level, p.image_path,
                       se.expiration_date, s.supplier_name
                FROM product p
                JOIN stock_entry se ON se.id_product = p.product_id
                JOIN stock_receipt sr ON sr.stock_receipt_id = se.id_stock_receipt
                JOIN supplier s ON s.supplier_id = sr.id_supplier
                WHERE p.sku LIKE '%-DEMO'
                ORDER BY p.sku
                """);
        assertEquals(EXPECTED_CATALOG.size(), rows.size());
        LocalDateTime catalogBaseTime = rows.stream()
                .filter(row -> "ARRO-0001-DEMO".equals(row.get("sku")))
                .map(row -> localDateTime(row.get("expiration_date")).minusMonths(18))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta vencimiento de referencia del catálogo demo"));

        for (ExpectedDemo expected : EXPECTED_CATALOG) {
            Map<String, Object> row = rows.stream()
                    .filter(candidate -> expected.sku().equals(candidate.get("sku")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Falta producto demo " + expected.sku()));
            assertEquals(expected.barcode(), trimmed(row.get("bar_code")), expected.sku());
            assertEquals(expected.saleType(), row.get("sale_type"), expected.sku());
            assertEquals(expected.category(), row.get("category"), expected.sku());
            assertDecimal(expected.cost(), row.get("cost"), expected.sku() + " costo");
            assertDecimal(expected.gain(), row.get("gain_amount"), expected.sku() + " ganancia");
            assertDecimal(expected.stock(), row.get("stock"), expected.sku() + " stock");
            assertDecimal(expected.reorder(), row.get("reorder_level"), expected.sku() + " reorden");
            assertEquals("products/" + expected.asset(), row.get("image_path"), expected.sku());
            assertEquals(expected.supplier(), row.get("supplier_name"), expected.sku() + " proveedor histórico");

            LocalDateTime expiration = localDateTime(row.get("expiration_date"));
            if (expected.expirationMonths() == 0) {
                assertNull(expiration, expected.sku());
            } else {
                assertEquals(catalogBaseTime.plusMonths(expected.expirationMonths()), expiration, expected.sku());
            }
        }
    }

    private void assertDecimal(String expected, Object actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo((BigDecimal) actual), message);
    }

    private String trimmed(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new AssertionError("Tipo JDBC inesperado para fecha: " + value.getClass().getName());
    }

    @Test
    void conflictingStableSkuAbortsTheWholeSeedTransaction() {
        insertConflictingProduct();

        assertThrows(IllegalStateException.class, this::runSeed);

        assertEquals(1L, demoProducts());
        assertEquals(0L, demoSuppliers());
        assertEquals(0L, count("SELECT COUNT(*) FROM stock_entry"));
    }

    private void runSeed() {
        transactions.executeWithoutResult(status -> {
            try {
                new DemoCatalogBootstrap(suppliers, products).run(new DefaultApplicationArguments());
            } catch (RuntimeException exception) {
                status.setRollbackOnly();
                throw exception;
            } catch (Exception exception) {
                status.setRollbackOnly();
                throw new IllegalStateException(exception);
            }
        });
    }

    private void insertConflictingProduct() {
        jdbcTemplate.update("""
                INSERT INTO product (
                    product_id, version, sku, product_name, gain_strategy, gain_amount,
                    stock, cost, reorder_level, bar_code, sale_type, category, registration_date, image_path
                ) VALUES (?, 0, 'ARRO-0001-DEMO', 'Identidad en conflicto', CAST('PORCENTAJE' AS gain_strategy),
                          20, 1, 1, 1, '7791999999999', CAST('UNIDAD' AS sale_type),
                          CAST('OTROS' AS product_category), ?, NULL)
                """, UUID.randomUUID(), LocalDateTime.now());
    }

    private long demoProducts() {
        return count("SELECT COUNT(*) FROM product WHERE sku LIKE '%-DEMO'");
    }

    private long demoSuppliers() {
        return count("""
                SELECT COUNT(*) FROM supplier
                WHERE supplier_name IN ('Andes Alimentos', 'Costa Bebidas', 'Sierra Lacteos',
                                        'Hogar Limpio', 'Cuidado Diario', 'Mundo Mascotas')
                """);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }

    private BigDecimal decimal(String sql) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    private record ExpectedDemo(String sku, String asset, String saleType, String category, String supplier,
                                String barcode, String cost, String gain, String stock, String reorder,
                                int expirationMonths) {}

    private static final List<ExpectedDemo> EXPECTED_CATALOG = List.of(
            e("ARRO-0001-DEMO", "arroz-extra.webp", "UNIDAD", "ABARROTES_SECOS", "Andes Alimentos", "7750000000001", "4.20", "25", "40", "10", 18),
            e("AZUC-0002-DEMO", "azucar-rubia.webp", "GRANEL", "ABARROTES_SECOS", "Andes Alimentos", null, "3.10", "30", "25.500", "5", 18),
            e("ACEI-0003-DEMO", "aceite-vegetal.webp", "UNIDAD", "ABARROTES_SECOS", "Andes Alimentos", "7750000000003", "8.50", "20", "24", "6", 12),
            e("AGUA-0004-DEMO", "agua-mineral.webp", "UNIDAD", "BEBIDAS", "Costa Bebidas", "7750000000004", "1.20", "40", "60", "12", 24),
            e("CAFE-0005-DEMO", "cafe-molido.webp", "UNIDAD", "CAFE_INFUSIONES", "Costa Bebidas", "7750000000005", "12.00", "25", "18", "5", 12),
            e("LECH-0006-DEMO", "leche-entera.webp", "UNIDAD", "LACTEOS", "Sierra Lacteos", "7750000000006", "3.40", "20", "30", "8", 6),
            e("DETE-0007-DEMO", "detergente-floral.webp", "UNIDAD", "LIMPIEZA_HOGAR", "Hogar Limpio", "7750000000007", "6.80", "30", "20", "5", 0),
            e("JABO-0008-DEMO", "jabon-neutro.webp", "UNIDAD", "CUIDADO_PERSONAL", "Cuidado Diario", "7750000000008", "2.10", "35", "36", "10", 0),
            e("PANA-0009-DEMO", "panales-bebe.webp", "UNIDAD", "BEBES", "Cuidado Diario", "7750000000009", "24.00", "20", "12", "4", 0),
            e("GALL-0010-DEMO", "galletas-vainilla.webp", "UNIDAD", "SNACKS_GOLOSINAS", "Costa Bebidas", "7750000000010", "1.80", "35", "48", "12", 10),
            e("ALIM-0011-DEMO", "alimento-canino.webp", "GRANEL", "MASCOTAS", "Mundo Mascotas", null, "7.50", "25", "35.000", "8", 12));

    private static ExpectedDemo e(String sku, String asset, String saleType, String category, String supplier,
                                  String barcode, String cost, String gain, String stock, String reorder,
                                  int expirationMonths) {
        return new ExpectedDemo(sku, asset, saleType, category, supplier, barcode, cost, gain, stock, reorder,
                expirationMonths);
    }
}
