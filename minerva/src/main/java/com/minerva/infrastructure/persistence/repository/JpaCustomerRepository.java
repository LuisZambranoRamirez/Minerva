package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    boolean existsByFullName(String fullName);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByRuc(String ruc);
    Optional<CustomerEntity> findByPhoneNumber(String phoneNumber);
    Optional<CustomerEntity> findByRuc(String ruc);
}

