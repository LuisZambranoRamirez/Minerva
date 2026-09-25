package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.SupplierEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
public interface JpaSupplierRepository extends JpaRepository<SupplierEntity, UUID> {
    Optional<SupplierEntity> findByRuc(String ruc);
    Optional<SupplierEntity> findByPhoneNumber(String phoneNumber);
    boolean existsByRuc(String ruc);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsBySupplierName(String supplierName);
}
