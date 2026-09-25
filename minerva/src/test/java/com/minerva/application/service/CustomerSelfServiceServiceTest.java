package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.application.port.drivers.CustomerSelfServiceUseCase;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.Role;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.domain.entities.order.Order;
import com.minerva.domain.entities.order.OrderId;
import com.minerva.domain.entities.order.OrderStatus;
import com.minerva.domain.entities.order.OrderTransition;
import com.minerva.domain.entities.product.InventoryLoss;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.repositories.OrderRepository;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.BarCode;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.ProductName;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.SKU;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.OrderDetailIdImpl;
import com.minerva.domain.valueObject.id.OrderIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSelfServiceServiceTest {

    @Test
    void catalogExposesOnlySafeFieldsWithCalculatedPriceAndAvailability() throws Exception {
        Fixture fixture = Fixture.approvedCliente();
        Product available = product("Producto Seguro", "5", "10.00", "2.50");
        Product soldOut = product("Producto Agotado", "0", "7.00", "1.00");
        fixture.products.put(available);
        fixture.products.put(soldOut);

        List<CustomerSelfServiceUseCase.CatalogItem> catalog = fixture.service().listCatalog();

        assertEquals(2, catalog.size());
        CustomerSelfServiceUseCase.CatalogItem item = catalog.stream()
                .filter(candidate -> candidate.productId().equals(available.getId().getIdValueAsString()))
                .findFirst().orElseThrow();
        assertEquals("ABCD-1234-EFGH", item.sku());
        assertEquals("Producto Seguro", item.productName());
        assertEquals("OTROS", item.category());
        assertEquals("GRANEL", item.saleType());
        assertEquals(available.getPrice().getValue(), item.price());
        assertEquals(CustomerSelfServiceUseCase.CatalogAvailability.DISPONIBLE, item.availability());
        assertEquals(CustomerSelfServiceUseCase.CatalogAvailability.AGOTADO,
                catalog.stream().filter(candidate -> candidate.productId().equals(soldOut.getId().getIdValueAsString())).findFirst().orElseThrow().availability());

        Set<String> exposedFields = java.util.Arrays.stream(CustomerSelfServiceUseCase.CatalogItem.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(exposedFields.contains("stock"));
        assertFalse(exposedFields.contains("cost"));
        assertFalse(exposedFields.contains("supplier"));
        assertFalse(exposedFields.contains("reorderLevel"));
    }

    @Test
    void checkoutDerivesOwnerAndPriceFromServerAndUsesDeliveryDefaults() throws Exception {
        Fixture fixture = Fixture.approvedCliente();
        Product product = product("Producto", "9", "10.00", "2.50");
        fixture.products.put(product);

        CustomerSelfServiceUseCase.OrderView order = fixture.service().checkout(new CustomerSelfServiceUseCase.CheckoutCommand(
                List.of(new CustomerSelfServiceUseCase.CheckoutItemCommand(product.getId().getIdValueAsString(), new BigDecimal("2"))),
                null,
                null,
                null
        ));

        Order saved = fixture.orders.saved.getFirst();
        assertEquals(fixture.customer.getId(), saved.getCustomerId(), "customer ownership must come from current JWT-linked user");
        assertEquals(product.getPrice().getValue(), saved.getDetails().getFirst().unitPrice().getValue(), "unit price must be server-calculated");
        assertEquals("Av. Entrega 123", order.deliveryAddress());
        assertEquals("Contacto Default", order.deliveryContact());
        assertEquals("987654321", order.deliveryPhone());
        assertEquals(
                0,
                new BigDecimal("25.0000").compareTo(order.details().getFirst().subtotal()),
                "subtotal must be numerically equal regardless of BigDecimal scale"
        );
    }

    @Test
    void checkoutRejectsMissingDeliveryWhenNoDefaultsExist() throws Exception {
        Fixture fixture = Fixture.approvedClienteWithoutDeliveryDefaults();
        Product product = product("Producto", "9", "10.00", "2.50");
        fixture.products.put(product);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> fixture.service().checkout(
                new CustomerSelfServiceUseCase.CheckoutCommand(
                        List.of(new CustomerSelfServiceUseCase.CheckoutItemCommand(product.getId().getIdValueAsString(), BigDecimal.ONE)),
                        " ",
                        null,
                        null
                )));

        assertTrue(error.getMessage().contains("dirección de entrega"));
        assertTrue(fixture.orders.saved.isEmpty());
    }

    @Test
    void ownOrderLookupUsesOwnershipPredicateAndBlocksIdor() throws Exception {
        Fixture fixture = Fixture.approvedCliente();
        Order otherOrder = orderFor(CustomerIdImpl.generate(), OrderStatus.PENDIENTE);
        fixture.orders.put(otherOrder);

        assertThrows(ResourceNotFoundException.class,
                () -> fixture.service().getOrder(otherOrder.getId().getIdValueAsString()));

        assertEquals(1, fixture.orders.ownedLookupCount);
        assertEquals(0, fixture.orders.unscopedLookupCount, "self-service must not perform unscoped ID lookups");
    }

    @Test
    void customerCancellationRequiresReasonUsesLockedOwnedLookupAndPersistsReason() throws Exception {
        Fixture fixture = Fixture.approvedCliente();
        Product product = product("Producto", "9", "10.00", "2.50");
        fixture.products.put(product);
        Order owned = orderFor(fixture.customer.getId(), OrderStatus.PENDIENTE, product);
        fixture.orders.put(owned);

        assertThrows(ConflictException.class,
                () -> fixture.service().cancelOrder(owned.getId().getIdValueAsString(), " "));
        assertEquals(1, fixture.orders.lockedOwnedLookupCount);

        fixture.service().cancelOrder(owned.getId().getIdValueAsString(), "Cliente se equivocó de producto");

        assertEquals(2, fixture.orders.lockedOwnedLookupCount);
        assertEquals(OrderStatus.CANCELADO, owned.getStatus());
        assertEquals("Cliente se equivocó de producto", owned.getTransitions().getLast().getCancellationReason());
    }

    @Test
    void checkoutCommandAndControllerRequestDoNotModelManipulableOwnerOrUnitPrice() {
        Set<String> commandFields = recordFieldNames(CustomerSelfServiceUseCase.CheckoutCommand.class);
        Set<String> itemFields = recordFieldNames(CustomerSelfServiceUseCase.CheckoutItemCommand.class);
        Set<String> controllerFields = recordFieldNames(com.minerva.infrastructure.rest.controller.CustomerSelfServiceController.CheckoutRequest.class);
        Set<String> controllerItemFields = recordFieldNames(com.minerva.infrastructure.rest.controller.CustomerSelfServiceController.CheckoutItemRequest.class);

        assertFalse(commandFields.contains("customerId"));
        assertFalse(commandFields.contains("unitPrice"));
        assertEquals(Set.of("productId", "quantity"), itemFields);
        assertFalse(controllerFields.contains("customerId"));
        assertFalse(controllerFields.contains("unitPrice"));
        assertEquals(Set.of("productId", "quantity"), controllerItemFields);
    }

    private static Set<String> recordFieldNames(Class<?> recordType) {
        return java.util.Arrays.stream(recordType.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Product product(String name, String stock, String cost, String gain) throws DomainException {
        return new Product("ABCD-1234-EFGH", name, GainStrategy.RECARGO_FIJO, new BigDecimal(gain), new BigDecimal("1"),
                null, SaleType.GRANEL, new BigDecimal(stock), ProductCategory.OTROS, new BigDecimal(cost));
    }

    private static Order orderFor(CustomerId customerId, OrderStatus status) throws DomainException {
        return orderFor(customerId, status, product("Restored", "10", "10.00", "2.50"));
    }

    private static Order orderFor(CustomerId customerId, OrderStatus status, Product product) throws DomainException {
        return new Order(OrderIdImpl.generate(), customerId, status, LocalDateTime.now().minusMinutes(3), LocalDateTime.now(),
                List.of(new Order.OrderItemRestoreDTO(OrderDetailIdImpl.generate(), product.getId(), new ProductQuantity(BigDecimal.ONE), product.getPrice())),
                List.of(), "Av. Entrega", "Contacto", "987654321");
    }

    private static class Fixture {
        final FakeUserRepository users = new FakeUserRepository();
        final FakeProductRepository products = new FakeProductRepository();
        final FakeOrderRepository orders = new FakeOrderRepository();
        final FakeCustomerRepository customers = new FakeCustomerRepository();
        final FakeInventoryMovementRepository movements = new FakeInventoryMovementRepository();
        final Customer customer;
        final User user;
        final CurrentUserProvider currentUserProvider;

        private Fixture(boolean withDeliveryDefaults) throws DomainException {
            this.customer = new Customer(CustomerIdImpl.generate(), new FullName("Cliente Empresa"), "987654321", LocalDateTime.now(),
                    "Acme", "Acme SAC", "20601234567", "Av. Fiscal",
                    withDeliveryDefaults ? "Av. Entrega 123" : null,
                    withDeliveryDefaults ? "Contacto Default" : null,
                    withDeliveryDefaults ? "987654321" : null);
            this.user = new User(new com.minerva.domain.entities.personal.Personal("12345678", "Cliente", "Uno", "987654321", Role.CLIENTE, "cliente@example.com"),
                    "cliente01", "hash", true, LocalDateTime.now(), customer.getId(), AccountApprovalStatus.APPROVED,
                    LocalDateTime.now(), new UserName("admin01"), null, null, null);
            this.currentUserProvider = () -> new UserContext("cliente01", "CLIENTE");
            users.put(user);
            customers.put(customer);
        }

        static Fixture approvedCliente() throws DomainException { return new Fixture(true); }
        static Fixture approvedClienteWithoutDeliveryDefaults() throws DomainException { return new Fixture(false); }

        CustomerSelfServiceService service() {
            return new CustomerSelfServiceService(users, currentUserProvider, products, orders, customers, movements);
        }
    }

    private static class FakeProductRepository implements ProductRepository {
        final Map<ProductId, Product> products = new HashMap<>();
        void put(Product product) { products.put(product.getId(), product); }
        @Override public void registerProduct(Product product, StockReceipt stockReceipt, StockEntry stockEntry) { put(product); }
        @Override public void save(Product product) { put(product); }
        @Override public void saveStockEntry(StockReceipt stockReceipt, StockEntry stockEntry, Product product) { put(product); }
        @Override public void saveUnitToBulk(ProductId unitProductId, ProductId bulkProductId, ProductQuantity quantity) { }
        @Override public void saveInventoryLoss(InventoryLoss inventoryLoss, Product product) { put(product); }
        @Override public boolean existsUnitToBulkByBulkProductId(ProductId bulkProductId) { return false; }
        @Override public boolean existsById(ProductId id) { return products.containsKey(id); }
        @Override public boolean existsBySku(SKU sku) { return false; }
        @Override public boolean existsByProductName(ProductName productName) { return false; }
        @Override public boolean existByBarCode(BarCode barCode) { return false; }
        @Override public Optional<Product> findById(ProductId id) { return Optional.ofNullable(products.get(id)); }
        @Override public Optional<Product> findByBarCode(BarCode barCode) { return Optional.empty(); }
        @Override public List<Product> findAllProducts() { return List.copyOf(products.values()); }
        @Override public List<Product> findLowStockProducts() { return List.of(); }
        @Override public List<StockEntry> findAllEntriesByProductId(ProductId id) { return List.of(); }
        @Override public Set<Product> findAllByIds(Set<ProductId> productIds) { return productIds.stream().map(products::get).collect(java.util.stream.Collectors.toSet()); }
        @Override public List<Product> findAllByIdsOrdered(Set<ProductId> productIds) { return productIds.stream().map(products::get).filter(java.util.Objects::nonNull).toList(); }
        @Override public List<InventoryLoss> findInventoryLossesByProductId(ProductId productId) { return List.of(); }
        @Override public List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason) { return List.of(); }
        @Override public List<InventoryLoss> findAllInventoryLosses() { return List.of(); }
    }

    private static class FakeOrderRepository implements OrderRepository {
        final Map<OrderId, Order> orders = new HashMap<>();
        final List<Order> saved = new ArrayList<>();
        int unscopedLookupCount;
        int ownedLookupCount;
        int lockedOwnedLookupCount;
        void put(Order order) { orders.put(order.getId(), order); }
        @Override public void save(Order order) { saved.add(order); put(order); }
        @Override public Optional<Order> findById(OrderId orderId) { unscopedLookupCount++; return Optional.ofNullable(orders.get(orderId)); }
        @Override public Optional<Order> findByIdForUpdate(OrderId orderId) { unscopedLookupCount++; return Optional.ofNullable(orders.get(orderId)); }
        @Override public Optional<Order> findByIdAndCustomerId(OrderId orderId, CustomerId customerId) { ownedLookupCount++; return Optional.ofNullable(orders.get(orderId)).filter(order -> order.getCustomerId().equals(customerId)); }
        @Override public Optional<Order> findByIdAndCustomerIdForUpdate(OrderId orderId, CustomerId customerId) { lockedOwnedLookupCount++; return Optional.ofNullable(orders.get(orderId)).filter(order -> order.getCustomerId().equals(customerId)); }
        @Override public List<Order> findByCustomerId(CustomerId customerId) { return orders.values().stream().filter(order -> order.getCustomerId().equals(customerId)).toList(); }
        @Override public List<Order> findAll(OrderStatus status, CustomerId customerId, LocalDateTime from, LocalDateTime to) { return orders.values().stream().filter(order -> order.getCustomerId().equals(customerId)).toList(); }
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        final Map<CustomerId, Customer> customers = new HashMap<>();
        void put(Customer customer) { customers.put(customer.getId(), customer); }
        @Override public void save(Customer customer) { put(customer); }
        @Override public boolean existsById(CustomerId id) { return customers.containsKey(id); }
        @Override public boolean existsByFullName(FullName fullName) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByRuc(String ruc) { return false; }
        @Override public Optional<Customer> findById(CustomerId id) { return Optional.ofNullable(customers.get(id)); }
        @Override public Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return List.copyOf(customers.values()); }
    }

    private static class FakeInventoryMovementRepository implements InventoryMovementRepository {
        final List<InventoryMovement> appended = new ArrayList<>();
        @Override public void append(InventoryMovement movement) { appended.add(movement); }
        @Override public void appendAll(List<InventoryMovement> movements) { appended.addAll(movements); }
        @Override public List<InventoryMovement> findAll(ProductId productId, InventoryMovementType type, InventoryMovementSource source, LocalDateTime from, LocalDateTime to, int page, int size) { return List.copyOf(appended); }
    }

    private static class FakeUserRepository implements UserRepository {
        final Map<UserId, User> users = new HashMap<>();
        final List<AuditEvent> audits = new ArrayList<>();
        void put(User user) { users.put(user.getId(), user); }
        @Override public void save(User user) { put(user); }
        @Override public void save(AuditEvent auditEvent) { audits.add(auditEvent); }
        @Override public boolean existsById(UserId id) { return users.containsKey(id); }
        @Override public boolean existsByDNI(DNI dni) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByEmail(Email email) { return false; }
        @Override public boolean existsByCustomerId(CustomerId customerId) { return users.values().stream().anyMatch(user -> customerId.equals(user.getCustomerId())); }
        @Override public boolean hasUsers() { return true; }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(users.get(id)); }
        @Override public List<User> findClienteAccounts(AccountApprovalStatus status) { return users.values().stream().filter(user -> user.getApprovalStatus() == status).toList(); }
    }
}
