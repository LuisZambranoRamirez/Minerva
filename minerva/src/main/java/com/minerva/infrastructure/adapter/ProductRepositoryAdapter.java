package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.product.InventoryLoss;
import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.entities.stockEntry.StockEntry;
import com.minerva.domain.entities.stockReceipt.StockReceipt;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.repositories.ProductRepository;
import com.minerva.domain.valueObject.BarCode;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductName;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.SKU;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.InventoryLossIdImpl;
import com.minerva.domain.valueObject.id.StockEntryIdImpl;
import com.minerva.domain.valueObject.id.StockReceiptIdImpl;
import com.minerva.infrastructure.persistence.entity.ProductEntity;
import com.minerva.infrastructure.persistence.entity.InventoryLossEntity;
import com.minerva.infrastructure.persistence.entity.StockEntryEntity;
import com.minerva.infrastructure.persistence.entity.StockReceiptEntity;
import com.minerva.infrastructure.persistence.entity.SupplierEntity;
import com.minerva.infrastructure.persistence.entity.UnitToBulkEntity;
import com.minerva.infrastructure.persistence.repository.JpaProductRepository;
import com.minerva.infrastructure.persistence.repository.JpaInventoryLossRepository;
import com.minerva.infrastructure.persistence.repository.JpaStockEntryRepository;
import com.minerva.infrastructure.persistence.repository.JpaStockReceiptRepository;
import com.minerva.infrastructure.persistence.repository.JpaUnitToBulkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaProductRepository jpaProductRepository;
    private final JpaStockReceiptRepository jpaStockReceiptRepository;
    private final JpaStockEntryRepository jpaStockEntryRepository;
    private final JpaUnitToBulkRepository jpaUnitToBulkRepository;
    private final JpaInventoryLossRepository jpaInventoryLossRepository;

    public ProductRepositoryAdapter(
            JpaProductRepository jpaProductRepository,
            JpaStockReceiptRepository jpaStockReceiptRepository,
            JpaStockEntryRepository jpaStockEntryRepository,
            JpaUnitToBulkRepository jpaUnitToBulkRepository,
            JpaInventoryLossRepository jpaInventoryLossRepository
    ) {
        this.jpaProductRepository = jpaProductRepository;
        this.jpaStockReceiptRepository = jpaStockReceiptRepository;
        this.jpaStockEntryRepository = jpaStockEntryRepository;
        this.jpaUnitToBulkRepository = jpaUnitToBulkRepository;
        this.jpaInventoryLossRepository = jpaInventoryLossRepository;
    }

    @Override
    @Transactional
    public void registerProduct(Product product, StockReceipt stockReceipt, StockEntry stockEntry) {
        ProductEntity productEntity = jpaProductRepository.save(toEntity(product));
        StockReceiptEntity receiptEntity = jpaStockReceiptRepository.save(toEntity(stockReceipt));
        jpaStockEntryRepository.save(toEntity(stockEntry, productEntity, receiptEntity));
    }

    @Override
    @Transactional
    public void save(Product product) {
        ProductEntity productEntity = getManagedProduct(product.getId());
        synchronizeMutableState(productEntity, product);
    }

    @Override
    @Transactional
    public void saveStockEntry(StockReceipt stockReceipt, StockEntry stockEntry, Product product) {
        ProductEntity productEntity = getManagedProduct(product.getId());
        synchronizeMutableState(productEntity, product);
        StockReceiptEntity receiptEntity = jpaStockReceiptRepository.save(toEntity(stockReceipt));
        jpaStockEntryRepository.save(toEntity(stockEntry, productEntity, receiptEntity));
    }

    @Override
    @Transactional
    public void saveUnitToBulk(ProductId unitProductId, ProductId bulkProductId, ProductQuantity quantity) {
        ProductEntity unitProduct = entityManager.getReference(ProductEntity.class, unitProductId.getIdValue());
        ProductEntity bulkProduct = entityManager.getReference(ProductEntity.class, bulkProductId.getIdValue());

        jpaUnitToBulkRepository.save(new UnitToBulkEntity(
                new UnitToBulkEntity.UnitToBulkId(bulkProductId.getIdValue(), unitProductId.getIdValue()),
                bulkProduct,
                unitProduct,
                quantity.getValue(),
                LocalDateTime.now()
        ));
    }

    @Override
    public boolean existsUnitToBulkByBulkProductId(ProductId bulkProductId) {
        return jpaUnitToBulkRepository.existsByBulkProduct_ProductId(bulkProductId.getIdValue());
    }

    @Override
    @Transactional
    public void saveInventoryLoss(InventoryLoss inventoryLoss, Product product) {
        ProductEntity productEntity = entityManager.find(ProductEntity.class, product.getId().getIdValue());
        if (productEntity == null) {
            throw new EntityRestoreException("No se encontró el producto al registrar la pérdida de inventario.");
        }

        productEntity.setStock(product.getStock().getValue());
        jpaInventoryLossRepository.save(new InventoryLossEntity(
                inventoryLoss.getId().getIdValue(),
                productEntity,
                inventoryLoss.getQuantity().getValue(),
                inventoryLoss.getReason(),
                inventoryLoss.getObservation().map(value -> value.getValue()).orElse(null),
                inventoryLoss.getRegistrationDate()
        ));
    }

    @Override
    public boolean existsById(ProductId id) {
        return jpaProductRepository.existsById(id.getIdValue());
    }

    @Override
    public boolean existsBySku(SKU sku) {
        return jpaProductRepository.existsBySku(sku.getValue());
    }

    @Override
    public boolean existsByProductName(ProductName productName) {
        return jpaProductRepository.existsByProductName(productName.getValue());
    }

    @Override
    public boolean existByBarCode(BarCode barCode) {
        return jpaProductRepository.existsByBarCode(barCode.getValue());
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpaProductRepository.findById(id.getIdValue()).map(this::toDomain);
    }

    @Override
    public Optional<Product> findByBarCode(BarCode barCode) {
        return jpaProductRepository.findByBarCode(barCode.getValue()).map(this::toDomain);
    }

    @Override
    public List<Product> findAllProducts() {
        return jpaProductRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Product> findLowStockProducts() {
        return jpaProductRepository.findLowStockProducts().stream().map(this::toDomain).toList();
    }

    @Override
    public List<StockEntry> findAllEntriesByProductId(ProductId id) {
        return jpaStockEntryRepository.findByProduct_ProductId(id.getIdValue())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Set<Product> findAllByIds(Set<ProductId> productIds) {
        List<java.util.UUID> ids = productIds.stream()
                .map(ProductId::getIdValue)
                .toList();

        return jpaProductRepository.findAllById(ids)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Product> findAllByIdsOrdered(Set<ProductId> productIds) {
        List<java.util.UUID> ids = productIds.stream().map(ProductId::getIdValue).sorted().toList();
        return jpaProductRepository.findAllByIdsOrdered(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public List<InventoryLoss> findInventoryLossesByProductId(ProductId productId) {
        return jpaInventoryLossRepository.findByProduct_ProductId(productId.getIdValue())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<InventoryLoss> findInventoryLossesByReason(InventoryLossReason reason) {
        return jpaInventoryLossRepository.findByReason(reason).stream().map(this::toDomain).toList();
    }

    @Override
    public List<InventoryLoss> findAllInventoryLosses() {
        return jpaInventoryLossRepository.findAll().stream().map(this::toDomain).toList();
    }

    private Product toDomain(ProductEntity entity) {
        try {
            return new Product(
                    new ProductIdImpl(entity.getProductId()),
                    new SKU(entity.getSku()),
                    new ProductName(entity.getProductName()),
                    entity.getGainStrategy(),
                    entity.getGainAmount(),
                    entity.getReorderLevel(),
                    entity.getBarCode(),
                    entity.getSaleType(),
                    new ProductQuantity(entity.getStock()),
                    entity.getCategory(),
                    new Money(entity.getCost()),
                    entity.getRegistrationDate()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el producto.", e);
        }
    }

    private ProductEntity toEntity(Product product) {
        return new ProductEntity(
                product.getId().getIdValue(),
                null,
                product.getSku().getValue(),
                product.getProductName().getValue(),
                product.getGainStrategy(),
                product.getGainAmount(),
                product.getStock().getValue(),
                product.getCost().getValue(),
                product.getReorderLevel().map(ProductQuantity::getValue).orElse(null),
                product.getBarCode().map(BarCode::getValue).orElse(null),
                product.getSaleType(),
                product.getCategory(),
                product.getRegistrationDate()
        );
    }

    private ProductEntity getManagedProduct(ProductId productId) {
        ProductEntity productEntity = entityManager.find(ProductEntity.class, productId.getIdValue());
        if (productEntity == null) {
            throw new EntityRestoreException("No se encontró el producto que se intentaba actualizar.");
        }
        return productEntity;
    }

    private void synchronizeMutableState(ProductEntity productEntity, Product product) {
        productEntity.setStock(product.getStock().getValue());
        productEntity.setCost(product.getCost().getValue());
    }

    private StockReceiptEntity toEntity(StockReceipt stockReceipt) {
        SupplierEntity supplier = entityManager.getReference(
                SupplierEntity.class,
                stockReceipt.getSupplierId().getIdValue()
        );

        return new StockReceiptEntity(
                stockReceipt.getId().getIdValue(),
                supplier,
                stockReceipt.getRegistrationDate()
        );
    }

    private StockEntryEntity toEntity(
            StockEntry stockEntry,
            ProductEntity product,
            StockReceiptEntity stockReceipt
    ) {
        return new StockEntryEntity(
                stockEntry.getId().getIdValue(),
                product,
                stockReceipt,
                stockEntry.getUnitPrice().getValue(),
                stockEntry.getQuantity().getValue(),
                stockEntry.getExpirationDate().orElse(null)
        );
    }

    private StockEntry toDomain(StockEntryEntity entity) {
        try {
            return new StockEntry(
                    new StockEntryIdImpl(entity.getStockEntryId()),
                    new StockReceiptIdImpl(entity.getStockReceipt().getStockReceiptId()),
                    new ProductIdImpl(entity.getProduct().getProductId()),
                    new Money(entity.getUnitPrice()),
                    new ProductQuantity(entity.getQuantity()),
                    entity.getExpirationDate()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar la entrada de stock.", e);
        }
    }

    private InventoryLoss toDomain(InventoryLossEntity entity) {
        try {
            return new InventoryLoss(
                    new InventoryLossIdImpl(entity.getInventoryLossId()),
                    new ProductIdImpl(entity.getProduct().getProductId()),
                    new ProductQuantity(entity.getQuantity()),
                    entity.getReason(),
                    entity.getObservation(),
                    entity.getRegistrationDate()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar la pérdida de inventario.", e);
        }
    }
}
