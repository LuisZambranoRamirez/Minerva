package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.minerva.integration.support.ExpectedFlywayMigrations.EXPECTED_VERSIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIsolationIntegrationTest extends IntegrationTest {

    private static final Set<String> EXPECTED_CUSTOMER_PERMISSIONS = Set.of(
            "CUSTOMER_CATALOG_READ",
            "CUSTOMER_ORDER_CREATE",
            "CUSTOMER_ORDER_FIND_BY_ID",
            "CUSTOMER_ORDER_FIND_ALL",
            "CUSTOMER_ORDER_FIND_TRANSITIONS",
            "CUSTOMER_ORDER_CANCEL"
    );
    private static final Set<String> EXPECTED_INVENTORY_REPORT_ROLES = Set.of(
            "ADMIN", "VENDEDOR", "ALMACENISTA"
    );

    private ScenarioObservation cleanStateObservation;

    @Test
    @Order(1)
    void representativeScenarioProducesKnownObservationFromCleanState() {
        assertNoProductStateFromPreviousScenarios();
        assertPreservedStateIsAvailable();

        cleanStateObservation = runRepresentativeScenario();

        assertEquals(0, cleanStateObservation.productsBefore());
        assertEquals(1, cleanStateObservation.productsAfter());
        assertEquals(IntegrationFixtures.PRODUCT_ID, cleanStateObservation.productId());
        assertEquals(0, new BigDecimal("5.000").compareTo(cleanStateObservation.stock()));
        assertEquals(cleanStateObservation.preservedBefore(), cleanStateObservation.preservedAfter());
    }

    @Test
    @Order(2)
    void stateMutatingPredecessorLeavesCommittedStateForTheNextCleanupBoundary() {
        assertNoProductStateFromPreviousScenarios();
        assertEquals(0, countProductsById(IntegrationFixtures.PRODUCT_ID));
        assertPreservedStateIsAvailable();

        fixtures.secondProduct(new BigDecimal("9.000"));

        assertEquals(1, countRows("product"));
        assertEquals(1, countProductsById(IntegrationFixtures.SECOND_PRODUCT_ID));
        assertPreservedStateIsAvailable();
    }

    @Test
    @Order(3)
    void inheritedCleanerRemovesPredecessorStateAndReproducesTheScenario() {
        assertNotNull(cleanStateObservation);
        assertNoProductStateFromPreviousScenarios();
        assertEquals(0, countProductsById(IntegrationFixtures.SECOND_PRODUCT_ID));
        assertPreservedStateIsAvailable();

        ScenarioObservation observationAfterPredecessorAndReset = runRepresentativeScenario();

        assertEquals(cleanStateObservation, observationAfterPredecessorAndReset);
    }

    private ScenarioObservation runRepresentativeScenario() {
        long productsBefore = countRows("product");
        PreservedState preservedBefore = preservedState();

        IntegrationFixtures.ProductFixture product = fixtures.product(new BigDecimal("5.000"));

        return new ScenarioObservation(
                productsBefore,
                preservedBefore,
                product.id(),
                countRows("product"),
                jdbcTemplate.queryForObject(
                        "SELECT stock FROM product WHERE product_id = ?",
                        BigDecimal.class,
                        product.id()
                ),
                preservedState()
        );
    }

    private void assertNoProductStateFromPreviousScenarios() {
        assertEquals(0, countRows("product"));
    }

    private void assertPreservedStateIsAvailable() {
        assertEquals(
                new PreservedState(EXPECTED_VERSIONS, EXPECTED_CUSTOMER_PERMISSIONS,
                        EXPECTED_INVENTORY_REPORT_ROLES, 1, 1),
                preservedState()
        );
    }

    private PreservedState preservedState() {
        List<String> flywayVersions = jdbcTemplate.queryForList("""
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank
                """, String.class);

        Set<String> customerPermissions = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT id_permission::text
                FROM role_permission
                WHERE id_role = CAST('CLIENTE' AS role)
                """, String.class));

        Set<String> inventoryReportRoles = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT id_role::text
                FROM role_permission
                WHERE id_permission = CAST('PRODUCT_EXPORT_INVENTORY_REPORT' AS permission)
                """, String.class));

        return new PreservedState(
                flywayVersions,
                customerPermissions,
                inventoryReportRoles,
                countRowsWhere("customer", "full_name", "anonimo"),
                countRowsWhere("supplier", "supplier_name", "anonimo")
        );
    }

    private long countProductsById(UUID productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE product_id = ?",
                Long.class,
                productId
        );
        return count == null ? 0 : count;
    }

    private long countRows(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count == null ? 0 : count;
    }

    private long countRowsWhere(String table, String column, String value) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                value
        );
        return count == null ? 0 : count;
    }

    private record PreservedState(
            List<String> flywayVersions,
            Set<String> customerPermissions,
            Set<String> inventoryReportRoles,
            long anonymousCustomers,
            long anonymousSuppliers
    ) {}

    private record ScenarioObservation(
            long productsBefore,
            PreservedState preservedBefore,
            UUID productId,
            long productsAfter,
            BigDecimal stock,
            PreservedState preservedAfter
    ) {}
}
