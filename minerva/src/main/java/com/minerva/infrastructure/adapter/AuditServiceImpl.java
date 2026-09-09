package com.minerva.infrastructure.adapter;

import com.minerva.application.port.driven.AuditService;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final UserRepository userRepository;

    @Override
    public void register(AuditEvent auditEvent) {
        userRepository.save(auditEvent);
    }
}