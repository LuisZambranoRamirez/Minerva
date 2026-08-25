package com.minerva.application.port.driven;

import com.minerva.domain.entities.userAction.UserAction;

public interface AuditService {
    void register(UserAction userAction);
}