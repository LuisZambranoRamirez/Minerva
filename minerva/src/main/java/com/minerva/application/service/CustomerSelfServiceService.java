package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.CustomerSelfServiceUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.order.Order;
import com.minerva.domain.entities.order.OrderId;
import com.minerva.domain.entities.order.OrderStatus;
import com.minerva.domain.entities.order.OrderTransition;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.repositories.OrderRepository;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.OrderIdImpl;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class CustomerSelfServiceService extends Service implements CustomerSelfServiceUseCase {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final InventoryMovementRepository movementRepository;
    private final UserRepository userRepository;

    public CustomerSelfServiceService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            InventoryMovementRepository movementRepository
    ) {
        super(userRepository, currentUserProvider);
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItem> listCatalog() {
        requireClientePermission(Permission.CUSTOMER_CATALOG_READ, "consultar el catálogo de clientes");
        List<CatalogItem> result = productRepository.findAllProducts().stream()
                .sorted(Comparator.comparing(product -> product.getProductName().getValue()))
                .map(this::toCatalogItem)
                .toList();
        registerUserAction(Permission.CUSTOMER_CATALOG_READ, new CollectionAuditTarget(CollectionAuditTarget.Resource.PRODUCTS));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItem getCatalogItem(String productId) {
        requireClientePermission(Permission.CUSTOMER_CATALOG_READ, "consultar el catálogo de clientes");
        try {
            Product product = productRepository.findById(ProductIdImpl.fromString(productId))
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
            registerUserAction(Permission.CUSTOMER_CATALOG_READ, product.getId());
            return toCatalogItem(product);
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public OrderView checkout(CheckoutCommand command) {
        requireClientePermission(Permission.CUSTOMER_ORDER_CREATE, "crear pedidos propios");
        if (command == null || command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un item.");
        }
        try {
            User currentUser = currentCliente();
            Customer customer = currentCustomer(currentUser);
            LinkedHashSet<ProductId> productIds = parseUniqueProductIds(command.items());
            List<Product> products = productRepository.findAllByIdsOrdered(productIds);
            if (products.size() != productIds.size()) {
                throw new ResourceNotFoundException("Uno o más productos no fueron encontrados.");
            }

            Map<ProductId, Product> byId = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            List<Order.OrderItemCreateDTO> orderItems = new ArrayList<>();

            for (CheckoutItemCommand item : command.items()) {
                ProductId productId = ProductIdImpl.fromString(item.productId());
                Product product = byId.get(productId);
                if (item.quantity() == null) {
                    throw new IllegalArgumentException("La cantidad del item no puede estar vacía.");
                }
                ProductQuantity requestedQuantity = new ProductQuantity(item.quantity());
                validateAvailability(product, requestedQuantity);
                Money authoritativePrice = product.getPrice();
                orderItems.add(new Order.OrderItemCreateDTO(productId, requestedQuantity, authoritativePrice));
            }

            Order order = new Order(
                    customer.getId(),
                    orderItems,
                    currentUser.getId(),
                    deliveryOrDefault(command.deliveryAddress(), customer.getDefaultDeliveryAddress(), "dirección de entrega"),
                    deliveryOrDefault(command.deliveryContact(), customer.getDefaultDeliveryContact(), "contacto de entrega"),
                    deliveryOrDefault(command.deliveryPhone(), customer.getDefaultDeliveryPhone(), "teléfono de entrega")
            );
            orderRepository.save(order);
            registerUserAction(Permission.CUSTOMER_ORDER_CREATE, order.getId());
            return toOrderView(order);
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderView> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) {
        requireClientePermission(Permission.CUSTOMER_ORDER_FIND_ALL, "consultar pedidos propios");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("El rango de fechas es inválido.");
        }
        CustomerId customerId = currentCustomerId();
        List<OrderView> result = orderRepository.findAll(status, customerId, from, to)
                .stream()
                .map(this::toOrderView)
                .toList();
        registerUserAction(Permission.CUSTOMER_ORDER_FIND_ALL, new CollectionAuditTarget(CollectionAuditTarget.Resource.ORDERS));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getOrder(String orderId) {
        requireClientePermission(Permission.CUSTOMER_ORDER_FIND_BY_ID, "consultar pedidos propios por ID");
        Order order = loadOwnedOrder(orderId, false);
        registerUserAction(Permission.CUSTOMER_ORDER_FIND_BY_ID, order.getId());
        return toOrderView(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTransitionView> listOrderTransitions(String orderId) {
        requireClientePermission(Permission.CUSTOMER_ORDER_FIND_TRANSITIONS, "consultar transiciones de pedidos propios");
        Order order = loadOwnedOrder(orderId, false);
        registerUserAction(Permission.CUSTOMER_ORDER_FIND_TRANSITIONS, order.getId());
        return order.getTransitions().stream().map(this::toTransitionView).toList();
    }

    @Override
    public OrderView cancelOrder(String orderId, String reason) {
        requireClientePermission(Permission.CUSTOMER_ORDER_CANCEL, "cancelar pedidos propios");
        Order order = loadOwnedOrder(orderId, true);
        UserId actor = actorId();
        try {
            boolean restoreStock = order.cancel(actor, reason);
            if (restoreStock) {
                restoreStockForCancelledOrder(order, actor);
            }
            orderRepository.save(order);
            registerUserAction(Permission.CUSTOMER_ORDER_CANCEL, order.getId());
            return toOrderView(order);
        } catch (DomainException e) {
            throw new ConflictException(e.getMessage());
        }
    }

    private void restoreStockForCancelledOrder(Order order, UserId actor) throws DomainException {
        List<Product> products = productsFor(order);
        Map<ProductId, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<InventoryMovement> movements = new ArrayList<>();
        for (Order.OrderItemReadDTO item : order.getDetails()) {
            Product product = byId.get(item.productId());
            ProductQuantity before = product.getStock();
            requireSuccess(product.increaseStock(item.quantity()));
            movements.add(new InventoryMovement(
                    product.getId(),
                    item.quantity().getValue(),
                    before,
                    product.getStock(),
                    InventoryMovementSource.ORDER_CANCELLATION,
                    order.getId().getIdValue(),
                    actor
            ));
        }
        products.forEach(productRepository::save);
        movementRepository.appendAll(movements);
    }

    private List<Product> productsFor(Order order) {
        LinkedHashSet<ProductId> ids = order.getDetails().stream()
                .map(Order.OrderItemReadDTO::productId)
                .sorted(Comparator.comparing(ProductId::getIdValue))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Product> products = productRepository.findAllByIdsOrdered(ids);
        if (products.size() != ids.size()) {
            throw new ResourceNotFoundException("Uno o más productos del pedido no existen.");
        }
        return products;
    }

    private void validateAvailability(Product product, ProductQuantity requestedQuantity) {
        if (requestedQuantity.isZeroOrLess()) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (product.getSaleType() == SaleType.UNIDAD && requestedQuantity.isDecimal()) {
            throw new IllegalArgumentException("Este producto se vende por unidad. Ingresá una cantidad entera.");
        }
        if (product.getStock().isZero()) {
            throw new ConflictException("El producto no está disponible.");
        }
        if (product.getStock().isLessThan(requestedQuantity)) {
            throw new ConflictException("La cantidad solicitada no está disponible.");
        }
        if (product.getPrice().isZeroOrLess()) {
            throw new ConflictException("El producto no tiene un precio de venta válido.");
        }
    }

    private Order loadOwnedOrder(String value, boolean lock) {
        try {
            OrderId orderId = OrderIdImpl.fromString(value);
            CustomerId customerId = currentCustomerId();
            Optional<Order> order = lock
                    ? orderRepository.findByIdAndCustomerIdForUpdate(orderId, customerId)
                    : orderRepository.findByIdAndCustomerId(orderId, customerId);
            return order.orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private LinkedHashSet<ProductId> parseUniqueProductIds(List<CheckoutItemCommand> items) throws DomainException {
        LinkedHashSet<ProductId> ids = new LinkedHashSet<>();
        for (CheckoutItemCommand item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Los items no pueden contener valores vacíos.");
            }
            ProductId id = ProductIdImpl.fromString(item.productId());
            if (!ids.add(id)) {
                throw new IllegalArgumentException("El pedido no puede contener productos duplicados.");
            }
        }
        return ids;
    }

    private User currentCliente() throws DomainException {
        User user = userRepository.findById(new UserName(getCurrentUser().userId()))
                .orElseThrow(() -> new UnauthorizedActionException("El usuario autenticado no existe."));
        if (user.getRole() != Role.CLIENTE || !user.isApproved() || user.getCustomerId() == null) {
            throw new UnauthorizedActionException("La cuenta de cliente no está habilitada para autoservicio.");
        }
        return user;
    }

    private Customer currentCustomer(User user) {
        return customerRepository.findById(user.getCustomerId())
                .orElseThrow(() -> new UnauthorizedActionException("La cuenta de cliente no tiene un cliente válido asociado."));
    }

    private CustomerId currentCustomerId() {
        try {
            return currentCliente().getCustomerId();
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private UserId actorId() {
        try {
            return new UserName(getCurrentUser().userId());
        } catch (DomainException e) {
            throw new IllegalStateException("El usuario autenticado es inválido.", e);
        }
    }

    private void requireClientePermission(Permission permission, String action) {
        Role role = getUserRole();
        if (role != Role.CLIENTE || role.lacksPermission(permission)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para " + action + ".");
        }
    }

    private String deliveryOrDefault(String requested, Optional<String> defaultValue, String label) {
        String normalized = normalize(requested);
        if (normalized == null) {
            normalized = defaultValue.map(CustomerSelfServiceService::normalize).orElse(null);
        }
        if (normalized == null) {
            throw new IllegalArgumentException("La " + label + " es obligatoria.");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CatalogItem toCatalogItem(Product product) {
        return new CatalogItem(
                product.getId().getIdValueAsString(),
                product.getSku().getValue(),
                product.getProductName().getValue(),
                product.getCategory().name(),
                product.getSaleType().name(),
                product.getPrice().getValue(),
                product.getStock().isGreaterThanZero() ? CatalogAvailability.DISPONIBLE : CatalogAvailability.AGOTADO
        );
    }

    private OrderView toOrderView(Order order) {
        List<OrderDetailView> details = order.getDetails().stream()
                .map(detail -> new OrderDetailView(
                        detail.orderDetailId().getIdValueAsString(),
                        detail.productId().getIdValueAsString(),
                        detail.quantity().getValue(),
                        detail.unitPrice().getValue(),
                        detail.unitPrice().getValue().multiply(detail.quantity().getValue())
                ))
                .toList();
        return new OrderView(
                order.getId().getIdValueAsString(),
                order.getStatus(),
                order.getRegistrationDate(),
                order.getUpdatedDate(),
                order.calculateTotal().getValue(),
                order.getDeliveryAddress(),
                order.getDeliveryContact(),
                order.getDeliveryPhone(),
                details
        );
    }

    private OrderTransitionView toTransitionView(OrderTransition transition) {
        return new OrderTransitionView(
                transition.getId().getIdValueAsString(),
                transition.getPreviousStatus(),
                transition.getNewStatus(),
                transition.getActorId().getIdValueAsString(),
                transition.getRegistrationDate(),
                transition.getCancellationReason()
        );
    }

    private void requireSuccess(Result<Void> result) {
        if (result.isFail()) throw new ConflictException(result.getMessage());
    }
}
