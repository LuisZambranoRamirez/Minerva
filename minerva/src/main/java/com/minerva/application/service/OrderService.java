package com.minerva.application.service;

import com.minerva.application.exceptions.*;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.OrderUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.entities.inventory.*;
import com.minerva.domain.entities.order.*;
import com.minerva.domain.entities.product.*;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.*;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.*;
import com.minerva.domain.valueObject.id.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class OrderService extends Service implements OrderUseCase {
    private final OrderRepository orderRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;

    public OrderService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                        OrderRepository orderRepository, InventoryMovementRepository movementRepository,
                        ProductRepository productRepository, CustomerRepository customerRepository,
                        SaleRepository saleRepository) {
        super(userRepository, currentUserProvider);
        this.orderRepository = orderRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    public Order create(CreateOrderCommand command) {
        requirePermission(Permission.ORDER_CREATE, "crear pedidos");
        if (command == null || command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un item.");
        }
        try {
            CustomerIdImpl customerId = CustomerIdImpl.fromString(command.customerId());
            if (customerRepository.findById(customerId).isEmpty()) throw new ResourceNotFoundException("Cliente no encontrado.");
            LinkedHashSet<ProductId> ids = parseUniqueProductIds(command.items());
            List<Product> products = productRepository.findAllByIdsOrdered(ids);
            if (products.size() != ids.size()) throw new ResourceNotFoundException("Uno o más productos no fueron encontrados.");
            Map<ProductId, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
            List<Order.OrderItemCreateDTO> items = new ArrayList<>();
            for (OrderItemCommand item : command.items()) {
                ProductId id = ProductIdImpl.fromString(item.productId());
                Money unitPrice = item.unitPrice() == null ? byId.get(id).getPrice() : new Money(item.unitPrice());
                items.add(new Order.OrderItemCreateDTO(id, new ProductQuantity(item.quantity()), unitPrice));
            }
            Order order = new Order(customerId, items, actorId());
            orderRepository.save(order);
            registerUserAction(Permission.ORDER_CREATE, order.getId());
            return order;
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override @Transactional(readOnly = true)
    public Order findById(String orderId) {
        requirePermission(Permission.ORDER_FIND_BY_ID, "consultar pedidos por ID");
        try {
            Order order = load(OrderIdImpl.fromString(orderId), false);
            registerUserAction(Permission.ORDER_FIND_BY_ID, order.getId());
            return order;
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override @Transactional(readOnly = true)
    public List<Order> findAll(OrderStatus status, String customerId, LocalDateTime from, LocalDateTime to) {
        requirePermission(Permission.ORDER_FIND_ALL, "consultar pedidos");
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("El rango de fechas es inválido.");
        try {
            CustomerIdImpl parsedCustomer = customerId == null || customerId.isBlank() ? null : CustomerIdImpl.fromString(customerId);
            if (parsedCustomer != null && customerRepository.findById(parsedCustomer).isEmpty()) {
                throw new ResourceNotFoundException("Cliente no encontrado.");
            }
            List<Order> orders = orderRepository.findAll(status, parsedCustomer, from, to);
            registerUserAction(Permission.ORDER_FIND_ALL, new CollectionAuditTarget(CollectionAuditTarget.Resource.ORDERS));
            return orders;
        } catch (DomainException e) { throw new IllegalArgumentException(e.getMessage(), e); }
    }

    @Override @Transactional(readOnly = true)
    public List<OrderTransition> findTransitions(String orderId) {
        requirePermission(Permission.ORDER_FIND_TRANSITIONS, "consultar transiciones de pedidos");
        Order order = findByIdWithoutAudit(orderId);
        registerUserAction(Permission.ORDER_FIND_TRANSITIONS, order.getId());
        return order.getTransitions();
    }

    @Override
    public Order confirm(String orderId) {
        requirePermission(Permission.ORDER_CONFIRM, "confirmar pedidos");
        Order order = locked(orderId);
        UserId actor = actorId();
        try {
            // Validate before touching stock so a rejected retry has no in-memory side effects.
            order.validateConfirmation(actor);
            List<Product> products = productsFor(order);
            Map<ProductId, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
            List<InventoryMovement> movements = new ArrayList<>();
            for (Order.OrderItemReadDTO item : order.getDetails()) {
                Product product = byId.get(item.productId());
                ProductQuantity before = product.getStock();
                requireSuccess(product.decreaseStock(item.quantity()));
                movements.add(new InventoryMovement(product.getId(), item.quantity().getValue().negate(), before,
                        product.getStock(), InventoryMovementSource.ORDER_CONFIRMATION,
                        order.getId().getIdValue(), actor));
            }
            order.confirm(actor);
            products.forEach(productRepository::save);
            movementRepository.appendAll(movements);
            orderRepository.save(order);
            registerUserAction(Permission.ORDER_CONFIRM, order.getId());
            return order;
        } catch (DomainException e) { throw conflict(e); }
    }

    @Override public Order prepare(String orderId) {
        requirePermission(Permission.ORDER_PREPARE, "preparar pedidos");
        return transition(orderId, Order::prepare, Permission.ORDER_PREPARE);
    }

    @Override public Order dispatch(String orderId) {
        requirePermission(Permission.ORDER_DISPATCH, "despachar pedidos");
        return transition(orderId, Order::dispatch, Permission.ORDER_DISPATCH);
    }

    @Override
    public Order cancel(String orderId) {
        requirePermission(Permission.ORDER_CANCEL, "cancelar pedidos");
        Order order = locked(orderId);
        UserId actor = actorId();
        try {
            boolean restore = order.cancel(actor);
            if (restore) {
                List<Product> products = productsFor(order);
                Map<ProductId, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
                List<InventoryMovement> movements = new ArrayList<>();
                for (Order.OrderItemReadDTO item : order.getDetails()) {
                    Product product = byId.get(item.productId());
                    ProductQuantity before = product.getStock();
                    requireSuccess(product.increaseStock(item.quantity()));
                    movements.add(new InventoryMovement(product.getId(), item.quantity().getValue(), before,
                            product.getStock(), InventoryMovementSource.ORDER_CANCELLATION,
                            order.getId().getIdValue(), actor));
                }
                products.forEach(productRepository::save);
                movementRepository.appendAll(movements);
            }
            orderRepository.save(order);
            registerUserAction(Permission.ORDER_CANCEL, order.getId());
            return order;
        } catch (DomainException e) { throw conflict(e); }
    }

    @Override
    public Order deliver(String orderId) {
        requirePermission(Permission.ORDER_DELIVER, "entregar pedidos");
        Order order = locked(orderId);
        if (saleRepository.findBySourceOrderId(order.getId()).isPresent()) {
            if (order.getStatus() == OrderStatus.ENTREGADO) {
                registerUserAction(Permission.ORDER_DELIVER, order.getId());
                return order;
            }
            throw new ConflictException("El pedido ya tiene una venta asociada.");
        }
        UserId actor = actorId();
        List<Product> products = productsFor(order);
        Map<ProductId, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        try {
            List<Sale.SaleItemCreateDTO> saleItems = order.getDetails().stream()
                    .map(item -> new Sale.SaleItemCreateDTO(byId.get(item.productId()),
                            item.quantity().getValue(), item.unitPrice().getValue())).toList();
            Sale sale = new Sale(order.getCustomerId(), saleItems, order.getId());
            order.deliver(actor);
            orderRepository.save(order);
            saleRepository.save(sale, Set.of());
            registerUserAction(Permission.ORDER_DELIVER, order.getId());
            return order;
        } catch (DomainException e) { throw conflict(e); }
    }

    private Order transition(String orderId, Transition action, Permission permission) {
        Order order = locked(orderId);
        try {
            action.apply(order, actorId());
            orderRepository.save(order);
            registerUserAction(permission, order.getId());
            return order;
        } catch (DomainException e) { throw conflict(e); }
    }

    private Order findByIdWithoutAudit(String orderId) {
        try { return load(OrderIdImpl.fromString(orderId), false); }
        catch (DomainException e) { throw new IllegalArgumentException(e.getMessage(), e); }
    }

    private Order locked(String value) {
        try { return load(OrderIdImpl.fromString(value), true); }
        catch (DomainException e) { throw new IllegalArgumentException(e.getMessage(), e); }
    }

    private Order load(OrderId id, boolean lock) {
        Optional<Order> result = lock ? orderRepository.findByIdForUpdate(id) : orderRepository.findById(id);
        return result.orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private List<Product> productsFor(Order order) {
        LinkedHashSet<ProductId> ids = order.getDetails().stream().map(Order.OrderItemReadDTO::productId)
                .sorted(Comparator.comparing(ProductId::getIdValue)).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Product> products = productRepository.findAllByIdsOrdered(ids);
        if (products.size() != ids.size()) throw new ResourceNotFoundException("Uno o más productos del pedido no existen.");
        return products;
    }

    private LinkedHashSet<ProductId> parseUniqueProductIds(List<OrderItemCommand> items) throws DomainException {
        LinkedHashSet<ProductId> ids = new LinkedHashSet<>();
        for (OrderItemCommand item : items) {
            if (item == null) throw new IllegalArgumentException("Los items no pueden contener valores vacíos.");
            ProductId id = ProductIdImpl.fromString(item.productId());
            if (!ids.add(id)) throw new IllegalArgumentException("El pedido no puede contener productos duplicados.");
        }
        return ids;
    }

    private UserId actorId() {
        try { return new UserName(getCurrentUser().userId()); }
        catch (DomainException e) { throw new IllegalStateException("El usuario autenticado es inválido.", e); }
    }

    private void requirePermission(Permission permission, String action) {
        if (getUserRole().lacksPermission(permission)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para " + action + ".");
        }
    }

    private void requireSuccess(Result<Void> result) {
        if (result.isFail()) throw new ConflictException(result.getMessage());
    }

    private ConflictException conflict(DomainException e) { return new ConflictException(e.getMessage()); }

    @FunctionalInterface private interface Transition { void apply(Order order, UserId actor) throws DomainException; }
}
