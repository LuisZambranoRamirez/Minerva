package com.minerva.domain.repositories;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.minerva.domain.entities.product.*;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.valueObject.BarCode;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.ProductName;
import com.minerva.domain.valueObject.SKU;
import com.minerva.domain.constants.InventoryLossReason;

public interface ProductRepository {
    void registerProduct(Product product, StockReceipt stockReceipt, StockEntry stockEntry);
    void save(Product product);
    void saveStockEntry(StockReceipt stockReceipt, StockEntry stockEntry, Product product);
    void saveUnitToBulk(ProductId unitProductId, ProductId bulkProductId, ProductQuantity quantity);
    void saveInventoryLoss(InventoryLoss inventoryLoss, Product product);
    boolean existsUnitToBulkByBulkProductId(ProductId bulkProductId);

    boolean existsById(ProductId id);
    boolean existsBySku(SKU sku);
    boolean existsByProductName(ProductName productName);
    boolean existByBarCode(BarCode barCode);
    Optional<Product> findById(ProductId id);
    Optional<Product> findByBarCode(BarCode barCode);
    List<Product> findAllProducts();
    List<Product> findLowStockProducts();
    List<StockEntry> findAllEntriesByProductId(ProductId id);
    Set<Product> findAllByIds(Set<ProductId> productIds);
    List<Product> findAllByIdsOrdered(Set<ProductId> productIds);
    List<InventoryLoss> findInventoryLossesByProductId(ProductId productId);
    List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason);
    List<InventoryLoss> findAllInventoryLosses();
}
