package com.minerva.application.port.drivers;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;
import com.minerva.domain.services.Result;
import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.entities.product.InventoryLoss;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductUseCase {
    // --------------------- WRITE ---------------------
    Result<Void> registerProduct(String sku,
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
                                 ) throws UnauthorizedActionException;

    Result<Void> registerStockEntry(String productId, String supplierNameId, BigDecimal unitPrice, BigDecimal quantity, LocalDateTime expirationDate) throws UnauthorizedActionException;
    Result<Void> registerUnitToBulk(String unitProductId, String bulkProductId, BigDecimal quantity) throws UnauthorizedActionException;
    Result<Void> registerInventoryLoss(String productId, BigDecimal quantity, InventoryLossReason reason, String observation) throws UnauthorizedActionException;

    // --------------------- READ ---------------------
    Optional<Product> findProductById(String productId) throws UnauthorizedActionException;
    Optional<Product> findProductByBarCode(String barCode) throws UnauthorizedActionException;
    List<Product> findAllProducts() throws UnauthorizedActionException;
    List<Product> findLowStockProducts() throws UnauthorizedActionException;
    List<InventoryMovement> findInventoryMovements(String productId, InventoryMovementType type,
                                                   InventoryMovementSource source, LocalDateTime from,
                                                   LocalDateTime to, int page, int size) throws UnauthorizedActionException;
    List<InventoryLoss> findInventoryLossesByProductId(String productId) throws UnauthorizedActionException;
    List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason) throws UnauthorizedActionException;
    List<InventoryLoss> findAllInventoryLosses() throws UnauthorizedActionException;
}
