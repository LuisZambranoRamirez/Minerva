package com.minerva.infrastructure.adapter;

import com.minerva.application.port.driven.AbuseAttemptRepository;
import com.minerva.infrastructure.persistence.entity.AbuseAttemptEntity;
import com.minerva.infrastructure.persistence.repository.JpaAbuseAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AbuseAttemptRepositoryAdapter implements AbuseAttemptRepository {
    private final JpaAbuseAttemptRepository repository;

    @Override
    @Transactional(readOnly = true)
    public long countRecent(String action, String subjectKey, LocalDateTime since) {
        return repository.countByActionAndSubjectKeyAndRegistrationDateGreaterThanEqual(action, subjectKey, since);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String subjectKey, boolean successful, LocalDateTime registrationDate) {
        repository.save(AbuseAttemptEntity.builder()
                .abuseAttemptId(UUID.randomUUID())
                .action(action)
                .subjectKey(subjectKey)
                .successful(successful)
                .registrationDate(registrationDate)
                .build());
    }
}