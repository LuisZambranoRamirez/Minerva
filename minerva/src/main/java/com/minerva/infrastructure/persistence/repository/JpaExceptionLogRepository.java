package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ExceptionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaExceptionLogRepository extends JpaRepository<ExceptionLogEntity, Long> {
}

