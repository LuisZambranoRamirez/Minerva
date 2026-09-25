package com.minerva.application.service;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.SaleUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.SaleRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.SaleIdImpl;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class SaleService extends Service implements SaleUseCase {
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    public SaleService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                       SaleRepository saleRepository, CustomerRepository customerRepository,
                       ProductRepository productRepository, InventoryMovementRepository movementRepository) {
        super(userRepository, currentUserProvider);
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public Result<Void> registerSale(String customerIdValue, List<PaymentCommand> payments,
                                     List<SaleItemCommand> items) throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_REGISTER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para registrar ventas.");
        }

        try {
            CustomerIdImpl customerId = CustomerIdImpl.fromString(customerIdValue);
            if (customerRepository.findById(customerId).isEmpty()) throw new ResourceNotFoundException("Cliente no encontrado.");
            if (items == null || items.isEmpty()) return Result.fail("La venta debe tener al menos un item.");

            Set<ProductId> productIds = new LinkedHashSet<>();
            for (SaleItemCommand item : items) {
                if (item == null) return Result.fail("Los items de la venta no pueden contener valores vacíos.");
                productIds.add(ProductIdImpl.fromString(item.productId()));
            }

            Set<Product> products = productRepository.findAllByIds(productIds);
            if (products.size() != productIds.size()) throw new ResourceNotFoundException("Uno o más productos no fueron encontrados.");

            Map<ProductId, Product> productsById = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            List<Sale.SaleItemCreateDTO> saleItems = new ArrayList<>(items.size());
            for (SaleItemCommand item : items) {
                ProductId productId = ProductIdImpl.fromString(item.productId());
                saleItems.add(new Sale.SaleItemCreateDTO(
                        productsById.get(productId),
                        item.quantity(),
                        item.unitPrice()
                ));
            }

            Sale sale = new Sale(customerId, saleItems);
            Result<Void> paymentResult = sale.addPayments(toPaymentDTOs(payments));
            if (paymentResult.isFail()) return paymentResult;

            List<InventoryMovement> movements = new ArrayList<>();
            UserId actor = actorId();
            for (Product product : products) {
                Optional<ProductQuantity> soldQuantity = sale.getSoldQuantity(product.getId());
                if (soldQuantity.isEmpty()) continue;
                ProductQuantity stockBefore = product.getStock();
                Result<Void> stockResult = product.decreaseStock(soldQuantity.get());
                if (stockResult.isFail()) return stockResult;
                movements.add(new InventoryMovement(
                        product.getId(),
                        soldQuantity.get().getValue().negate(),
                        stockBefore,
                        product.getStock(),
                        InventoryMovementSource.DIRECT_SALE,
                        sale.getId().getIdValue(),
                        actor
                ));
            }

            saleRepository.save(sale, products);
            movementRepository.appendAll(movements);
            registerUserAction(Permission.SALE_REGISTER, sale.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    public Result<Void> addPaymentToSale(String saleIdValue, List<PaymentCommand> payments)
            throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_ADD_PAYMENT)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para agregar pagos a ventas.");
        }
        try {
            SaleIdImpl saleId = SaleIdImpl.fromString(saleIdValue);
            Optional<Sale> saleResult = saleRepository.findById(saleId);
            if (saleResult.isEmpty()) throw new ResourceNotFoundException("Venta no encontrada.");

            Sale sale = saleResult.get();
            Result<Void> paymentResult = sale.addPayments(toPaymentDTOs(payments));
            if (paymentResult.isFail()) return paymentResult;

            saleRepository.updatePayments(sale);
            registerUserAction(Permission.SALE_ADD_PAYMENT, sale.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    public Optional<Sale> findSaleById(String saleId) throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_FIND_BY_ID)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para buscar ventas por ID.");
        }
        try {
            return saleRepository.findById(SaleIdImpl.fromString(saleId)).map(sale -> {
                registerUserAction(Permission.SALE_FIND_BY_ID, sale.getId());
                return sale;
            });
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public List<Sale> findSalesByCustomerId(String customerId) throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_FIND_BY_CUSTOMER_ID)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para buscar ventas por ID de cliente.");
        }
        try {
            CustomerIdImpl parsedCustomerId = CustomerIdImpl.fromString(customerId);
            if (!customerRepository.existsById(parsedCustomerId)) {
                throw new ResourceNotFoundException("Cliente no encontrado.");
            }
            return saleRepository.findByCustomerId(parsedCustomerId).stream()
                    .peek(sale -> registerUserAction(Permission.SALE_FIND_BY_CUSTOMER_ID, sale.getId())).toList();
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public List<Sale> findAllSales() throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_FIND_ALL)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para buscar todas las ventas.");
        }
        List<Sale> sales = saleRepository.findAll();
        registerUserAction(Permission.SALE_FIND_ALL,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.SALES));
        return sales;
    }

    @Transactional
    public Result<Void> registerProductReturn(String saleDetailIdValue, BigDecimal quantityValue,
                                              ProductReturnReason reason)
            throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_REGISTER_PRODUCT_RETURN)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para registrar devoluciones.");
        }
        try {
            SaleDetailIdImpl saleDetailId = SaleDetailIdImpl.fromString(saleDetailIdValue);
            ProductQuantity quantity = new ProductQuantity(quantityValue);
            ProductReturn productReturn = new ProductReturn(saleDetailId, quantity, reason);
            Optional<SaleRepository.ReturnContext> contextResult =
                    saleRepository.findReturnContextForUpdate(saleDetailId);
            if (contextResult.isEmpty()) throw new ResourceNotFoundException("Detalle de venta no encontrado.");

            SaleRepository.ReturnContext context = contextResult.get();
            ProductQuantity accumulated = context.returnedQuantity().add(quantity);
            if (accumulated.isGreaterThan(context.soldQuantity())) {
                return Result.fail("La cantidad acumulada devuelta no puede superar la cantidad vendida.");
            }

            Optional<Product> productResult = productRepository.findById(context.productId());
            if (productResult.isEmpty()) throw new ResourceNotFoundException("Producto de la venta no encontrado.");
            Product product = productResult.get();
            ProductQuantity stockBefore = product.getStock();
            if (productReturn.restoresStock()) {
                Result<Void> stockResult = product.increaseStock(quantity);
                if (stockResult.isFail()) return stockResult;
            }

            saleRepository.saveProductReturn(productReturn, product);
            if (productReturn.restoresStock()) {
                movementRepository.append(new InventoryMovement(
                        product.getId(),
                        quantity.getValue(),
                        stockBefore,
                        product.getStock(),
                        InventoryMovementSource.PRODUCT_RETURN,
                        productReturn.getId().getIdValue(),
                        actorId()
                ));
            }
            registerUserAction(Permission.SALE_REGISTER_PRODUCT_RETURN, productReturn.getId());
            return Result.success(null);
        } catch (DomainException exception) {
            return Result.fail(exception.getMessage());
        }
    }

    public List<ProductReturn> findProductReturnsBySaleDetailId(String saleDetailIdValue)
            throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_FIND_PRODUCT_RETURNS)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para consultar devoluciones.");
        }
        try {
            SaleDetailIdImpl saleDetailId = SaleDetailIdImpl.fromString(saleDetailIdValue);
            if (!saleRepository.existsSaleDetailById(saleDetailId)) {
                throw new ResourceNotFoundException("Detalle de venta no encontrado.");
            }
            List<ProductReturn> returns = saleRepository.findProductReturnsBySaleDetailId(saleDetailId);
            registerUserAction(Permission.SALE_FIND_PRODUCT_RETURNS, saleDetailId);
            return returns;
        } catch (DomainException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    public List<ProductReturn> findAllProductReturns() throws UnauthorizedActionException {
        if (getUserRole().lacksPermission(Permission.SALE_FIND_PRODUCT_RETURNS)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para consultar devoluciones.");
        }
        List<ProductReturn> returns = saleRepository.findAllProductReturns();
        registerUserAction(Permission.SALE_FIND_PRODUCT_RETURNS,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.PRODUCT_RETURNS));
        return returns;
    }

    private UserId actorId() {
        try {
            return new UserName(getCurrentUser().userId());
        } catch (DomainException e) {
            throw new IllegalStateException("El usuario autenticado es inválido.", e);
        }
    }

    private List<Sale.PayCreateDTO> toPaymentDTOs(List<PaymentCommand> payments) {
        if (payments == null) return null;
        return payments.stream().map(payment -> payment == null ? null
                : new Sale.PayCreateDTO(payment.amount(), payment.paymentMethod())).toList();
    }
}
