package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
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
import com.minerva.domain.entities.product.InventoryLoss;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.sale.SaleId;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.repositories.OrderRepository;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.SaleRepository;
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
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    @Test
    void confirmDecrementsStockOncePersistsNegativeMovementAndRejectsRetry() throws DomainException {
        Fixture fixture = Fixture.withRole(Role.VENDEDOR);
        Product product = fixture.productWithStock("10");
        Order order = fixture.order(OrderStatus.PENDIENTE, product, "3");
        fixture.orders.put(order);
        fixture.products.put(product);

        Order confirmed = fixture.service().confirm(order.getId().getIdValueAsString());

        assertEquals(OrderStatus.CONFIRMADO, confirmed.getStatus());
        assertEquals(new BigDecimal("7"), product.getStock().getValue());
        assertEquals(1, fixture.products.saved.size());
        assertEquals(1, fixture.movements.appended.size());
        InventoryMovement movement = fixture.movements.appended.get(0);
        assertEquals(InventoryMovementSource.ORDER_CONFIRMATION, movement.getSource());
        assertEquals(InventoryMovementType.SALIDA, movement.getType());
        assertEquals(new BigDecimal("-3"), movement.getQuantity());
        assertEquals(new BigDecimal("10"), movement.getStockBefore().getValue());
        assertEquals(new BigDecimal("7"), movement.getStockAfter().getValue());

        assertThrows(ConflictException.class, () -> fixture.service().confirm(order.getId().getIdValueAsString()));
        assertEquals(new BigDecimal("7"), product.getStock().getValue(), "a rejected retry must not decrement stock again");
        assertEquals(1, fixture.movements.appended.size());
    }

    @Test
    void cancelConfirmedOrderRestoresStockOnceAndCancellingPendingDoesNotTouchStock() throws DomainException {
        Fixture fixture = Fixture.withRole(Role.VENDEDOR);
        Product confirmedProduct = fixture.productWithStock("7");
        Order confirmed = fixture.order(OrderStatus.CONFIRMADO, confirmedProduct, "3");
        fixture.orders.put(confirmed);
        fixture.products.put(confirmedProduct);

        fixture.service().cancel(confirmed.getId().getIdValueAsString());

        assertEquals(OrderStatus.CANCELADO, confirmed.getStatus());
        assertEquals(new BigDecimal("10"), confirmedProduct.getStock().getValue());
        assertEquals(1, fixture.movements.appended.size());
        assertEquals(InventoryMovementSource.ORDER_CANCELLATION, fixture.movements.appended.get(0).getSource());
        assertEquals(new BigDecimal("3"), fixture.movements.appended.get(0).getQuantity());

        Product pendingProduct = fixture.productWithStock("10");
        Order pending = fixture.order(OrderStatus.PENDIENTE, pendingProduct, "3");
        fixture.orders.put(pending);
        fixture.products.put(pendingProduct);
        fixture.service().cancel(pending.getId().getIdValueAsString());

        assertEquals(OrderStatus.CANCELADO, pending.getStatus());
        assertEquals(new BigDecimal("10"), pendingProduct.getStock().getValue());
        assertEquals(1, fixture.movements.appended.size(), "pending cancellation must not write restock movements");
    }

    @Test
    void deliveryCreatesExactlyOneLinkedSaleWithoutChangingStockAndRetryIsIdempotent() throws DomainException {
        Fixture fixture = Fixture.withRole(Role.ALMACENISTA);
        Product product = fixture.productWithStock("7");
        Order order = fixture.order(OrderStatus.EN_REPARTO, product, "3");
        fixture.orders.put(order);
        fixture.products.put(product);

        fixture.service().deliver(order.getId().getIdValueAsString());

        assertEquals(OrderStatus.ENTREGADO, order.getStatus());
        assertEquals(new BigDecimal("7"), product.getStock().getValue(), "delivery must not decrement stock again");
        assertEquals(1, fixture.sales.saved.size());
        assertTrue(fixture.sales.findBySourceOrderId(order.getId()).isPresent());
        assertEquals(0, fixture.movements.appended.size(), "delivery must not write inventory movements");

        fixture.service().deliver(order.getId().getIdValueAsString());

        assertEquals(1, fixture.sales.saved.size(), "retried delivery must not create duplicate Sale");
        assertEquals(new BigDecimal("7"), product.getStock().getValue());
    }

    @Test
    void permissionsDenyUnauthorizedTransitionsBeforeMutatingState() throws DomainException {
        Fixture fixture = Fixture.withRole(Role.VENDEDOR);
        Product product = fixture.productWithStock("7");
        Order order = fixture.order(OrderStatus.CONFIRMADO, product, "3");
        fixture.orders.put(order);
        fixture.products.put(product);

        assertThrows(UnauthorizedActionException.class, () -> fixture.service().prepare(order.getId().getIdValueAsString()));
        assertEquals(OrderStatus.CONFIRMADO, order.getStatus());
        assertEquals(0, fixture.orders.saveCount);
        assertEquals(0, fixture.products.saved.size());
        assertEquals(0, fixture.movements.appended.size());
    }

    private static class Fixture {
        final FakeOrderRepository orders = new FakeOrderRepository();
        final FakeInventoryMovementRepository movements = new FakeInventoryMovementRepository();
        final FakeProductRepository products = new FakeProductRepository();
        final FakeCustomerRepository customers = new FakeCustomerRepository();
        final FakeSaleRepository sales = new FakeSaleRepository();
        final FakeUserRepository users = new FakeUserRepository();
        final CurrentUserProvider currentUserProvider;

        private Fixture(Role role) {
            this.currentUserProvider = () -> new UserContext("tester1", role.name());
        }

        static Fixture withRole(Role role) {
            return new Fixture(role);
        }

        OrderService service() {
            return new OrderService(users, currentUserProvider, orders, movements, products, customers, sales);
        }

        Product productWithStock(String stock) throws DomainException {
            return new Product(
                    "ABCD-1234-EFGH",
                    "Producto " + System.nanoTime(),
                    GainStrategy.RECARGO_FIJO,
                    new BigDecimal("1.00"),
                    new BigDecimal("5"),
                    null,
                    SaleType.GRANEL,
                    new BigDecimal(stock),
                    ProductCategory.OTROS,
                    new BigDecimal("10.00")
            );
        }

        Order order(OrderStatus status, Product product, String quantity) throws DomainException {
            return new Order(
                    OrderIdImpl.generate(),
                    customers.customer.getId(),
                    status,
                    LocalDateTime.now().minusMinutes(10),
                    LocalDateTime.now().minusMinutes(1),
                    List.of(new Order.OrderItemRestoreDTO(
                            OrderDetailIdImpl.generate(),
                            product.getId(),
                            new ProductQuantity(new BigDecimal(quantity)),
                            new Money(new BigDecimal("15.00"))
                    )),
                    List.of()
            );
        }
    }

    private static class FakeOrderRepository implements OrderRepository {
        final Map<OrderId, Order> orders = new HashMap<>();
        int saveCount;

        void put(Order order) { orders.put(order.getId(), order); }

        @Override public void save(Order order) { saveCount++; put(order); }
        @Override public Optional<Order> findById(OrderId orderId) { return Optional.ofNullable(orders.get(orderId)); }
        @Override public Optional<Order> findByIdForUpdate(OrderId orderId) { return findById(orderId); }
        @Override public Optional<Order> findByIdAndCustomerId(OrderId orderId, CustomerId customerId) { return findById(orderId).filter(order -> order.getCustomerId().equals(customerId)); }
        @Override public Optional<Order> findByIdAndCustomerIdForUpdate(OrderId orderId, CustomerId customerId) { return findByIdAndCustomerId(orderId, customerId); }
        @Override public List<Order> findByCustomerId(CustomerId customerId) { return orders.values().stream().filter(order -> order.getCustomerId().equals(customerId)).toList(); }
        @Override public List<Order> findAll(OrderStatus status, CustomerId customerId, LocalDateTime from, LocalDateTime to) { return List.copyOf(orders.values()); }
    }

    private static class FakeProductRepository implements ProductRepository {
        final Map<ProductId, Product> products = new HashMap<>();
        final List<Product> saved = new ArrayList<>();

        void put(Product product) { products.put(product.getId(), product); }

        @Override public void registerProduct(Product product, StockReceipt stockReceipt, StockEntry stockEntry) { put(product); }
        @Override public void save(Product product) { saved.add(product); put(product); }
        @Override public void saveStockEntry(StockReceipt stockReceipt, StockEntry stockEntry, Product product) { save(product); }
        @Override public void saveUnitToBulk(ProductId unitProductId, ProductId bulkProductId, ProductQuantity quantity) { }
        @Override public void saveInventoryLoss(InventoryLoss inventoryLoss, Product product) { save(product); }
        @Override public boolean existsUnitToBulkByBulkProductId(ProductId bulkProductId) { return false; }
        @Override public boolean existsById(ProductId id) { return products.containsKey(id); }
        @Override public boolean existsBySku(SKU sku) { return false; }
        @Override public boolean existsByProductName(ProductName productName) { return false; }
        @Override public boolean existByBarCode(BarCode barCode) { return false; }
        @Override public Optional<Product> findById(ProductId id) { return Optional.ofNullable(products.get(id)); }
        @Override public Optional<Product> findByBarCode(BarCode barCode) { return Optional.empty(); }
        @Override public List<Product> findAllProducts() { return List.copyOf(products.values()); }
        @Override public List<Product> findLowStockProducts() {
            return products.values().stream()
                    .filter(product -> product.getReorderLevel().isPresent())
                    .filter(product -> product.getStock().getValue().compareTo(product.getReorderLevel().orElseThrow().getValue()) <= 0)
                    .sorted(Comparator.comparing(product -> product.getProductName().getValue()))
                    .toList();
        }
        @Override public List<StockEntry> findAllEntriesByProductId(ProductId id) { return List.of(); }
        @Override public Set<Product> findAllByIds(Set<ProductId> productIds) {
            return productIds.stream().map(products::get).collect(java.util.stream.Collectors.toSet());
        }
        @Override public List<Product> findAllByIdsOrdered(Set<ProductId> productIds) {
            return productIds.stream()
                    .sorted(Comparator.comparing(ProductId::getIdValue))
                    .map(products::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        @Override public List<InventoryLoss> findInventoryLossesByProductId(ProductId productId) { return List.of(); }
        @Override public List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason) { return List.of(); }
        @Override public List<InventoryLoss> findAllInventoryLosses() { return List.of(); }
    }

    private static class FakeInventoryMovementRepository implements InventoryMovementRepository {
        final List<InventoryMovement> appended = new ArrayList<>();
        @Override public void append(InventoryMovement movement) { appended.add(movement); }
        @Override public void appendAll(List<InventoryMovement> movements) { appended.addAll(movements); }
        @Override public List<InventoryMovement> findAll(ProductId productId, InventoryMovementType type, InventoryMovementSource source, LocalDateTime from, LocalDateTime to, int page, int size) { return List.copyOf(appended); }
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        final Customer customer;

        FakeCustomerRepository() {
            try { this.customer = new Customer("Cliente Prueba", null); }
            catch (DomainException e) { throw new IllegalStateException(e); }
        }

        @Override public void save(Customer customer) { }
        @Override public boolean existsById(CustomerId id) { return customer.getId().equals(id); }
        @Override public boolean existsByFullName(FullName fullName) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByRuc(String ruc) { return customer.getRuc().map(ruc::equals).orElse(false); }
        @Override public Optional<Customer> findById(CustomerId id) { return existsById(id) ? Optional.of(customer) : Optional.empty(); }
        @Override public Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return List.of(customer); }
    }

    private static class FakeSaleRepository implements SaleRepository {
        final List<Sale> saved = new ArrayList<>();
        final Map<OrderId, Sale> byOrder = new HashMap<>();

        @Override public void save(Sale sale, Set<Product> products) {
            saved.add(sale);
            sale.getSourceOrderId().ifPresent(orderId -> byOrder.put(orderId, sale));
        }
        @Override public Optional<Sale> findBySourceOrderId(OrderId orderId) { return Optional.ofNullable(byOrder.get(orderId)); }
        @Override public Optional<Sale> findById(SaleId id) { return saved.stream().filter(sale -> sale.getId().equals(id)).findFirst(); }
        @Override public List<Sale> findByCustomerId(CustomerId customerId) { return List.of(); }
        @Override public List<Sale> findAll() { return List.copyOf(saved); }
        @Override public void updatePayments(Sale sale) { }
        @Override public Optional<SaleRepository.ReturnContext> findReturnContextForUpdate(SaleDetailId saleDetailId) { return Optional.empty(); }
        @Override public boolean existsSaleDetailById(SaleDetailId saleDetailId) { return false; }
        @Override public void saveProductReturn(ProductReturn productReturn, Product product) { }
        @Override public List<ProductReturn> findProductReturnsBySaleDetailId(SaleDetailId saleDetailId) { return List.of(); }
        @Override public List<ProductReturn> findAllProductReturns() { return List.of(); }
    }

    private static class FakeUserRepository implements UserRepository {
        final List<AuditEvent> auditEvents = new ArrayList<>();
        @Override public void save(User user) { }
        @Override public void save(AuditEvent auditEvent) { auditEvents.add(auditEvent); }
        @Override public boolean existsById(UserId id) { return true; }
        @Override public boolean existsByDNI(DNI dni) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByEmail(Email email) { return false; }
        @Override public boolean existsByCustomerId(CustomerId customerId) { return false; }
        @Override public boolean hasUsers() { return true; }
        @Override public Optional<User> findById(UserId id) { return Optional.empty(); }
        @Override public List<User> findClienteAccounts(com.minerva.domain.entities.user.AccountApprovalStatus status) { return List.of(); }
    }
}
