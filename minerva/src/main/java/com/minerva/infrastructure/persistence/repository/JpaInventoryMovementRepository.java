package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.InventoryMovementEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.*;

public interface JpaInventoryMovementRepository extends JpaRepository<InventoryMovementEntity, UUID>,
        JpaSpecificationExecutor<InventoryMovementEntity> {
}
