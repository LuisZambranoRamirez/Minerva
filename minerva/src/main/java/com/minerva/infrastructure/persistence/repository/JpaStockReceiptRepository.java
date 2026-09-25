package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.StockReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaStockReceiptRepository extends JpaRepository<StockReceiptEntity, UUID> {
}
