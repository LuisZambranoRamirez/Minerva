package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByBarCode(String barCode);
    boolean existsByBarCode(String barCode);
}
