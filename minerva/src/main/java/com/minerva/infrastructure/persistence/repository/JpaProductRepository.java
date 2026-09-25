package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByBarCode(String barCode);
    boolean existsByBarCode(String barCode);
    boolean existsBySku(String sku);
    boolean existsByProductName(String productName);

    @Query("select p from ProductEntity p where p.productId in :ids order by p.productId")
    List<ProductEntity> findAllByIdsOrdered(@Param("ids") Collection<UUID> ids);

    @Query("select p from ProductEntity p where p.reorderLevel is not null and p.stock <= p.reorderLevel order by p.productName")
    List<ProductEntity> findLowStockProducts();
}
