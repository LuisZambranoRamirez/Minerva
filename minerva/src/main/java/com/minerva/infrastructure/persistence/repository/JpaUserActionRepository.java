package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserActionRepository extends JpaRepository<AuditEventEntity, UUID> {
}
