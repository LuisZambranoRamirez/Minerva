package com.minerva.application.service;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.ProductUseCase;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.InventoryLoss;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.entities.supplier.SupplierId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.repositories.SupplierRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.BarCode;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.SupplierIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import com.minerva.domain.valueObject.id.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ProductService extends Service implements ProductUseCase {

    private static final int MAX_MOVEMENT_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryMovementRepository movementRepository;

    public ProductService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            InventoryMovementRepository movementRepository
    ) {
        super(userRepository, currentUserProvider);
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    public Result<Void> registerProduct(
            String sku,
            String productName,
            GainStrategy gainStrategy,
            BigDecimal gainAmount,
            BigDecimal reorderLevel,
            String barCode,
            SaleType saleType,
            ProductCategory productCategory,
            String purchasedFromSupplierId,
            BigDecimal purchaseUnitPrice,
            BigDecimal purchaseQuantity,
            LocalDateTime purchaseExpirationDate
    ) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_REGISTER, "registrar productos");

        try {
            SupplierId supplierId = SupplierIdImpl.fromString(purchasedFromSupplierId);
            if (!supplierRepository.existsById(supplierId)) {
                throw new ResourceNotFoundException("Proveedor no encontrado.");
            }

            Product product = new Product(
                    sku,
                    productName,
                    gainStrategy,
                    gainAmount,
                    reorderLevel,
                    barCode,
                    saleType,
                    purchaseQuantity,
                    productCategory,
                    purchaseUnitPrice
            );

            if (productRepository.existsBySku(product.getSku())) {
                return Result.fail("Ya existe un producto con el mismo SKU.");
            }
            if (productRepository.existsByProductName(product.getProductName())) {
                return Result.fail("Ya existe un producto con el mismo nombre.");
            }
            if (product.getBarCode().isPresent() && productRepository.existByBarCode(product.getBarCode().get())) {
                return Result.fail("Ya existe un producto con el mismo código de barras.");
            }

            StockReceipt stockReceipt = new StockReceipt(supplierId);
            StockEntry stockEntry = new StockEntry(
                    stockReceipt.getId(),
                    product.getId(),
                    purchaseUnitPrice,
                    purchaseQuantity,
                    purchaseExpirationDate
            );

            productRepository.registerProduct(product, stockReceipt, stockEntry);
            movementRepository.append(new InventoryMovement(
                    product.getId(),
                    product.getStock().getValue(),
                    new ProductQuantity(BigDecimal.ZERO),
                    product.getStock(),
                    InventoryMovementSource.STOCK_ENTRY,
                    stockEntry.getId().getIdValue(),
                    actorId()
            ));
            registerUserAction(Permission.PRODUCT_REGISTER, product.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<Void> registerStockEntry(
            String productId,
            String supplierId,
            BigDecimal unitPrice,
            BigDecimal quantity,
            LocalDateTime expirationDate
    ) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_REGISTER_STOCK_ENTRY, "registrar entradas de stock");

        try {
            ProductIdImpl parsedProductId = ProductIdImpl.fromString(productId);
            SupplierIdImpl parsedSupplierId = SupplierIdImpl.fromString(supplierId);

            Optional<Product> productOptional = productRepository.findById(parsedProductId);
            if (productOptional.isEmpty()) {
                throw new ResourceNotFoundException("Producto no encontrado.");
            }
            if (!supplierRepository.existsById(parsedSupplierId)) {
                throw new ResourceNotFoundException("Proveedor no encontrado.");
            }

            Product product = productOptional.get();
            StockReceipt stockReceipt = new StockReceipt(parsedSupplierId);
            StockEntry stockEntry = new StockEntry(
                    stockReceipt.getId(),
                    parsedProductId,
                    unitPrice,
                    quantity,
                    expirationDate
            );

            ProductQuantity stockBefore = product.getStock();
            Result<Void> stockResult = product.processDeliveryFromSupplier(stockEntry);
            if (stockResult.isFail()) {
                return stockResult;
            }

            productRepository.saveStockEntry(stockReceipt, stockEntry, product);
            movementRepository.append(new InventoryMovement(
                    product.getId(),
                    stockEntry.getQuantity().getValue(),
                    stockBefore,
                    product.getStock(),
                    InventoryMovementSource.STOCK_ENTRY,
                    stockEntry.getId().getIdValue(),
                    actorId()
            ));
            registerUserAction(Permission.PRODUCT_REGISTER_STOCK_ENTRY, stockEntry.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Result<Void> registerUnitToBulk(
            String unitProductId,
            String bulkProductId,
            BigDecimal quantity
    ) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_ASSOCIATE_UNIT_TO_BULK, "asociar productos por unidad y a granel");

        try {
            ProductIdImpl parsedUnitId = ProductIdImpl.fromString(unitProductId);
            ProductIdImpl parsedBulkId = ProductIdImpl.fromString(bulkProductId);

            Optional<Product> unitProductOptional = productRepository.findById(parsedUnitId);
            if (unitProductOptional.isEmpty()) throw new ResourceNotFoundException("No se encontró el producto vendido por unidad.");

            Optional<Product> bulkProductOptional = productRepository.findById(parsedBulkId);
            if (bulkProductOptional.isEmpty()) throw new ResourceNotFoundException("No se encontró el producto vendido a granel.");

            Product unitProduct = unitProductOptional.get();
            Product bulkProduct = bulkProductOptional.get();
            ProductQuantity productQuantity = new ProductQuantity(quantity);

            Result<Void> associationResult = unitProduct.addBulkAssociation(bulkProduct, productQuantity);
            if (associationResult.isFail()) return associationResult;

            if (productRepository.existsUnitToBulkByBulkProductId(parsedBulkId)) {
                return Result.fail("El producto vendido a granel ya está asociado a un producto por unidad.");
            }

            productRepository.saveUnitToBulk(unitProduct.getId(), bulkProduct.getId(), productQuantity);
            registerUserAction(Permission.PRODUCT_ASSOCIATE_UNIT_TO_BULK, unitProduct.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> registerInventoryLoss(
            String productId,
            BigDecimal quantity,
            InventoryLossReason reason,
            String observation
    ) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_REGISTER_INVENTORY_LOSS, "registrar pérdidas de inventario");

        try {
            ProductIdImpl parsedProductId = ProductIdImpl.fromString(productId);
            Optional<Product> productOptional = productRepository.findById(parsedProductId);
            if (productOptional.isEmpty()) {
                throw new ResourceNotFoundException("Producto no encontrado.");
            }

            ProductQuantity lossQuantity = new ProductQuantity(quantity);
            Product product = productOptional.get();
            ProductQuantity stockBefore = product.getStock();
            Result<Void> stockResult = product.decreaseStock(lossQuantity);
            if (stockResult.isFail()) {
                return stockResult;
            }

            InventoryLoss inventoryLoss = new InventoryLoss(
                    parsedProductId,
                    lossQuantity,
                    reason,
                    observation
            );
            productRepository.saveInventoryLoss(inventoryLoss, product);
            movementRepository.append(new InventoryMovement(
                    product.getId(),
                    lossQuantity.getValue().negate(),
                    stockBefore,
                    product.getStock(),
                    InventoryMovementSource.INVENTORY_LOSS,
                    inventoryLoss.getId().getIdValue(),
                    actorId()
            ));
            registerUserAction(Permission.PRODUCT_REGISTER_INVENTORY_LOSS, inventoryLoss.getId());
            return Result.success(null);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public Optional<Product> findProductById(String productId) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_BY_ID, "buscar productos por ID");

        try {
            return productRepository.findById(ProductIdImpl.fromString(productId))
                    .map(product -> {
                        registerUserAction(Permission.PRODUCT_FIND_BY_ID, product.getId());
                        return product;
                    });
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findProductByBarCode(String barCode) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_BY_BAR_CODE, "buscar productos por código de barras");

        try {
            return productRepository.findByBarCode(new BarCode(barCode))
                    .map(product -> {
                        registerUserAction(Permission.PRODUCT_FIND_BY_BAR_CODE, product.getId());
                        return product;
                    });
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public List<Product> findAllProducts() throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_ALL, "buscar todos los productos");

        List<Product> products = productRepository.findAllProducts();
        registerUserAction(
                Permission.PRODUCT_FIND_ALL,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.PRODUCTS)
        );
        return products;
    }

    @Override
    public List<Product> findLowStockProducts() throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_LOW_STOCK, "consultar productos con bajo stock");
        List<Product> products = productRepository.findLowStockProducts();
        registerUserAction(
                Permission.PRODUCT_FIND_LOW_STOCK,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.PRODUCTS)
        );
        return products;
    }

    @Override
    public List<InventoryMovement> findInventoryMovements(String productId, InventoryMovementType type,
                                                          InventoryMovementSource source, LocalDateTime from,
                                                          LocalDateTime to, int page, int size)
            throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_INVENTORY_MOVEMENTS, "consultar movimientos de inventario");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("El rango de fechas es inválido.");
        }
        if (page < 0) throw new IllegalArgumentException("La página no puede ser negativa.");
        if (size < 1 || size > MAX_MOVEMENT_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_MOVEMENT_PAGE_SIZE + ".");
        }

        try {
            ProductIdImpl parsedProductId = productId == null || productId.isBlank() ? null : ProductIdImpl.fromString(productId);
            if (parsedProductId != null && !productRepository.existsById(parsedProductId)) {
                throw new ResourceNotFoundException("Producto no encontrado.");
            }
            List<InventoryMovement> movements = movementRepository.findAll(parsedProductId, type, source, from, to, page, size);
            Id<?> auditTarget = parsedProductId == null
                    ? new CollectionAuditTarget(CollectionAuditTarget.Resource.INVENTORY_MOVEMENTS)
                    : parsedProductId;
            registerUserAction(Permission.PRODUCT_FIND_INVENTORY_MOVEMENTS, auditTarget);
            return movements;
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public List<InventoryLoss> findInventoryLossesByProductId(String productId) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_INVENTORY_LOSSES, "consultar pérdidas de inventario");
        try {
            ProductIdImpl parsedProductId = ProductIdImpl.fromString(productId);
            if (!productRepository.existsById(parsedProductId)) {
                throw new ResourceNotFoundException("Producto no encontrado.");
            }
            List<InventoryLoss> losses = productRepository.findInventoryLossesByProductId(parsedProductId);
            registerUserAction(Permission.PRODUCT_FIND_INVENTORY_LOSSES, parsedProductId);
            return losses;
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason) throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_INVENTORY_LOSSES, "consultar pérdidas de inventario");
        List<InventoryLoss> losses = productRepository.findInventoryLossesByReason(reason);
        registerUserAction(
                Permission.PRODUCT_FIND_INVENTORY_LOSSES,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.INVENTORY_LOSSES)
        );
        return losses;
    }

    @Override
    public List<InventoryLoss> findAllInventoryLosses() throws UnauthorizedActionException {
        requirePermission(Permission.PRODUCT_FIND_INVENTORY_LOSSES, "consultar pérdidas de inventario");
        List<InventoryLoss> losses = productRepository.findAllInventoryLosses();
        registerUserAction(
                Permission.PRODUCT_FIND_INVENTORY_LOSSES,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.INVENTORY_LOSSES)
        );
        return losses;
    }

    private UserId actorId() {
        try {
            return new UserName(getCurrentUser().userId());
        } catch (DomainException e) {
            throw new IllegalStateException("El usuario autenticado es inválido.", e);
        }
    }

    private void requirePermission(Permission permission, String action) {
        if (getUserRole().lacksPermission(permission)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para " + action + ".");
        }
    }
}
