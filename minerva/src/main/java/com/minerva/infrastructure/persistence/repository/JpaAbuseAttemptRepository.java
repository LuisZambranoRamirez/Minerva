package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.AbuseAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface JpaAbuseAttemptRepository extends JpaRepository<AbuseAttemptEntity, UUID> {
    long countByActionAndSubjectKeyAndRegistrationDateGreaterThanEqual(String action, String subjectKey, LocalDateTime since);
}