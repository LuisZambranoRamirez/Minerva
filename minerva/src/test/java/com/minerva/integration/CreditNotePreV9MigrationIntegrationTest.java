package com.minerva.integration;

import com.minerva.integration.support.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CreditNotePreV9MigrationIntegrationTest extends IntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private PostgreSQLContainer postgres;

    @Test
    void preV9SalesPaymentsAndReturnsAreBackfilledWithoutChangingHistoricalFinancialInputs() throws Exception {
        String database = "pre_v9_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String migrationUrl = databaseUrl(database);
        try (Connection connection = DriverManager.getConnection(
                migrationUrl, postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {

            Flyway.configure().dataSource(migrationUrl, postgres.getUsername(), postgres.getPassword())
                    .target("8").load().migrate();

            statement.executeUpdate("""
                    INSERT INTO customer (customer_id, full_name, phone_number, registration_date)
                    VALUES ('10000000-0000-0000-0000-000000000099', 'Cliente histórico', '911111199', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO product (product_id, version, sku, product_name, gain_strategy, gain_amount,
                                         stock, cost, reorder_level, bar_code, sale_type, category, registration_date)
                    VALUES ('20000000-0000-0000-0000-000000000099', 0, 'HIST-99', 'Producto histórico',
                            'PORCENTAJE', 20.00, 10.000, 10.00, 2.000, '7790000000099', 'UNIDAD', 'OTROS', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO sale (sale_id, id_customer, source_order_id, registration_date)
                    VALUES ('40000000-0000-0000-0000-000000000099',
                            '10000000-0000-0000-0000-000000000099', NULL, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO sale_detail (sale_detail_id, id_sale, id_product, quantity, unit_price)
                    VALUES ('41000000-0000-0000-0000-000000000099',
                            '40000000-0000-0000-0000-000000000099',
                            '20000000-0000-0000-0000-000000000099', 2.000, 12.00)
                    """);
            statement.executeUpdate("""
                    INSERT INTO pay (pay_id, id_sale, amount, payment_method, registration_date)
                    VALUES ('42000000-0000-0000-0000-000000000099',
                            '40000000-0000-0000-0000-000000000099', 5.00, 'EFECTIVO', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO product_return (product_return_id, id_sale_detail, quantity, reason, registration_date)
                    VALUES ('43000000-0000-0000-0000-000000000099',
                            '41000000-0000-0000-0000-000000000099', 0.500, 'DAÑADO', CURRENT_TIMESTAMP)
                    """);

            BigDecimal grossBefore = scalar(statement,
                    "SELECT quantity * unit_price FROM sale_detail WHERE sale_detail_id = '41000000-0000-0000-0000-000000000099'");
            BigDecimal paidBefore = scalar(statement,
                    "SELECT amount FROM pay WHERE pay_id = '42000000-0000-0000-0000-000000000099'");

            Flyway.configure().dataSource(migrationUrl, postgres.getUsername(), postgres.getPassword())
                    .load().migrate();

            assertEquals(0, grossBefore.compareTo(scalar(statement,
                    "SELECT quantity * unit_price FROM sale_detail WHERE sale_detail_id = '41000000-0000-0000-0000-000000000099'")));
            assertEquals(0, paidBefore.compareTo(scalar(statement,
                    "SELECT amount FROM pay WHERE pay_id = '42000000-0000-0000-0000-000000000099'")));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM credit_note"));
            assertEquals(1, count(statement, "SELECT COUNT(*) FROM product_return WHERE credit_note_id = product_return_id"));
            assertEquals(0, new BigDecimal("13.00").compareTo(scalar(statement, """
                    SELECT GREATEST(
                        SUM(sd.quantity * sd.unit_price)
                        - (SELECT COALESCE(SUM(amount), 0) FROM pay WHERE id_sale = s.sale_id)
                        - (SELECT COALESCE(SUM(pr.quantity * detail.unit_price), 0)
                           FROM product_return pr
                           JOIN sale_detail detail ON detail.sale_detail_id = pr.id_sale_detail
                           WHERE detail.id_sale = s.sale_id), 0)
                    FROM sale s JOIN sale_detail sd ON sd.id_sale = s.sale_id
                    WHERE s.sale_id = '40000000-0000-0000-0000-000000000099'
                    GROUP BY s.sale_id
                    """)));
            try (ResultSet result = statement.executeQuery(
                    "SELECT issued_by, reference FROM credit_note WHERE credit_note_id = '43000000-0000-0000-0000-000000000099'")) {
                result.next();
                assertEquals(null, result.getString("issued_by"));
                assertFalse(result.getString("reference").isBlank());
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '"
                        + database + "'");
                statement.execute("DROP DATABASE IF EXISTS " + database);
            }
        }
    }

    private String databaseUrl(String database) {
        String url = postgres.getJdbcUrl();
        int query = url.indexOf('?');
        String suffix = query < 0 ? "" : url.substring(query);
        String base = query < 0 ? url : url.substring(0, query);
        return base.substring(0, base.lastIndexOf('/') + 1) + database + suffix;
    }

    private BigDecimal scalar(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBigDecimal(1);
        }
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
