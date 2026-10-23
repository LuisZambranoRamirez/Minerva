package com.minerva.integration.support;

import com.minerva.domain.constants.Role;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.valueObject.Password;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class IntegrationFixtures {

    public static final String PASSWORD = "Password123!";
    public static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID SECOND_CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID SECOND_PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID ORDER_DETAIL_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    public static final UUID SECOND_ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    public static final UUID SECOND_ORDER_DETAIL_ID = UUID.fromString("31000000-0000-0000-0000-000000000002");
    public static final UUID SALE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID SALE_DETAIL_ID = UUID.fromString("41000000-0000-0000-0000-000000000001");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasher passwordHasher;

    public IntegrationFixtures(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
    }

    public UserFixture admin() {
        return createUser("admin01", "10000001", "900000001", "admin01@minerva.test", Role.ADMIN, true, null,
                "APPROVED");
    }

    public UserFixture seller() {
        return createUser("vendedor01", "10000002", "900000002", "vendedor01@minerva.test", Role.VENDEDOR,
                true, null, "APPROVED");
    }

    public UserFixture warehouseUser() {
        return createUser("almacen01", "10000003", "900000003", "almacen01@minerva.test",
                Role.ALMACENISTA, true, null, "APPROVED");
    }

    public UserFixture approvedCustomerUser() {
        CustomerFixture customer = customer();
        return createUser("cliente01", "10000004", "900000004", "cliente01@minerva.test", Role.CLIENTE, true,
                customer.id(), "APPROVED");
    }

    public UserFixture secondApprovedCustomerUser() {
        CustomerFixture customer = secondCustomer();
        return createUser("cliente02", "10000005", "900000005", "cliente02@minerva.test", Role.CLIENTE, true,
                customer.id(), "APPROVED");
    }

    public UserFixture pendingCustomerUser() {
        CustomerFixture customer = customer();
        return createUser("cliente01", "10000004", "900000004", "cliente01@minerva.test", Role.CLIENTE, true,
                customer.id(), "PENDING_APPROVAL");
    }

    public UserFixture disabledAdmin() {
        return createUser("admin01", "10000001", "900000001", "admin01@minerva.test", Role.ADMIN, false, null,
                "APPROVED");
    }

    public CustomerFixture customer() {
        return createCustomer(
                CUSTOMER_ID, "Cliente Integración", "911111111", "20600000001",
                "Minerva Test", "Minerva Test SAC", "Av. Pruebas 100",
                "Av. Entregas 200", "Contacto Test", "922222222"
        );
    }

    public CustomerFixture secondCustomer() {
        return createCustomer(
                SECOND_CUSTOMER_ID, "Cliente Integración Dos", "911111112", "20600000002",
                "Minerva Test Dos", "Minerva Test Dos SAC", "Av. Pruebas 101",
                "Av. Entregas 201", "Contacto Test Dos", "922222223"
        );
    }

    private CustomerFixture createCustomer(
            UUID id,
            String fullName,
            String phone,
            String ruc,
            String businessName,
            String legalName,
            String address,
            String deliveryAddress,
            String deliveryContact,
            String deliveryPhone
    ) {
        jdbcTemplate.update("""
                INSERT INTO customer (
                    customer_id, full_name, phone_number, registration_date,
                    business_name, legal_name, ruc, address,
                    default_delivery_address, default_delivery_contact, default_delivery_phone
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                fullName,
                phone,
                FIXED_TIME,
                businessName,
                legalName,
                ruc,
                address,
                deliveryAddress,
                deliveryContact,
                deliveryPhone
        );
        return new CustomerFixture(id, fullName);
    }

    public ProductFixture product(BigDecimal stock) {
        return product(PRODUCT_ID, "MINV-0001-TEST", "Producto Integración", "7790000000001", stock);
    }

    public ProductFixture secondProduct(BigDecimal stock) {
        return product(SECOND_PRODUCT_ID, "MINV-0002-TEST", "Alfa Integración", "7790000000002", stock);
    }

    public ProductFixture product(UUID id, String sku, String productName, String barCode, BigDecimal stock) {
        jdbcTemplate.update("""
                INSERT INTO product (
                    product_id, version, sku, product_name, gain_strategy, gain_amount,
                    stock, cost, reorder_level, bar_code, sale_type, category, registration_date
                ) VALUES (?, 0, ?, ?, CAST(? AS gain_strategy), ?, ?, ?, ?, ?,
                          CAST(? AS sale_type), CAST(? AS product_category), ?)
                """,
                id,
                sku,
                productName,
                "PORCENTAJE",
                new BigDecimal("20.00"),
                stock,
                new BigDecimal("10.00"),
                new BigDecimal("2.000"),
                barCode,
                "UNIDAD",
                "OTROS",
                FIXED_TIME
        );
        return new ProductFixture(id, stock);
    }

    public OrderFixture order(CustomerFixture customer, ProductFixture product) {
        return order(ORDER_ID, ORDER_DETAIL_ID, customer, product, "PENDIENTE");
    }

    public OrderFixture secondOrder(CustomerFixture customer, ProductFixture product, String status) {
        return order(SECOND_ORDER_ID, SECOND_ORDER_DETAIL_ID, customer, product, status);
    }

    public OrderFixture order(
            UUID orderId,
            UUID orderDetailId,
            CustomerFixture customer,
            ProductFixture product,
            String status
    ) {
        jdbcTemplate.update("""
                INSERT INTO customer_order (
                    order_id, id_customer, status, registration_date, updated_date,
                    delivery_address, delivery_contact, delivery_phone
                ) VALUES (?, ?, CAST(? AS order_status), ?, ?, ?, ?, ?)
                """,
                orderId, customer.id(), status, FIXED_TIME, FIXED_TIME,
                "Av. Entregas 200", "Contacto Test", "922222222"
        );
        jdbcTemplate.update("""
                INSERT INTO order_detail (order_detail_id, id_order, id_product, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """,
                orderDetailId, orderId, product.id(), new BigDecimal("2.000"), new BigDecimal("12.00")
        );
        return new OrderFixture(orderId, orderDetailId);
    }

    public SaleFixture sale(CustomerFixture customer, ProductFixture product) {
        jdbcTemplate.update("""
                INSERT INTO sale (sale_id, id_customer, source_order_id, registration_date)
                VALUES (?, ?, NULL, ?)
                """, SALE_ID, customer.id(), FIXED_TIME);
        jdbcTemplate.update("""
                INSERT INTO sale_detail (sale_detail_id, id_sale, id_product, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """,
                SALE_DETAIL_ID, SALE_ID, product.id(), new BigDecimal("1.000"), new BigDecimal("12.00")
        );
        return new SaleFixture(SALE_ID, SALE_DETAIL_ID);
    }

    private UserFixture createUser(
            String username,
            String dni,
            String phone,
            String email,
            Role role,
            boolean active,
            UUID customerId,
            String approvalStatus
    ) {
        jdbcTemplate.update("""
                INSERT INTO personal (
                    dni, names, lastnames, phone_number, role_name, email, is_active, registration_date
                ) VALUES (?, ?, ?, ?, CAST(? AS role), ?, ?, ?)
                """,
                dni, "Usuario", "Integración", phone, role.name(), email, active, FIXED_TIME
        );
        jdbcTemplate.update("""
                INSERT INTO app_user (
                    user_name, dni, password, is_active, registration_date, customer_id, approval_status
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS account_approval_status))
                """,
                username, dni, passwordHash(), active, FIXED_TIME, customerId, approvalStatus
        );
        return new UserFixture(username, PASSWORD, role, customerId);
    }

    private String passwordHash() {
        try {
            return passwordHasher.hash(new Password(PASSWORD)).getValue();
        } catch (DomainException exception) {
            throw new IllegalStateException("Invalid deterministic fixture password", exception);
        }
    }

    public record UserFixture(String username, String password, Role role, UUID customerId) {}

    public record CustomerFixture(UUID id, String fullName) {}

    public record ProductFixture(UUID id, BigDecimal stock) {}

    public record OrderFixture(UUID id, UUID detailId) {}

    public record SaleFixture(UUID id, UUID detailId) {}
}
