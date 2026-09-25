package com.minerva.application.service;

import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.repositories.UserRepository;

public abstract class Service {
    private static final String SPRING_ROLE_PREFIX = "ROLE_";

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    protected Service(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        if (userRepository == null) {
            throw new IllegalArgumentException("El repositorio de usuarios no puede ser nulo.");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("El proveedor del usuario actual no puede ser nulo.");
        }

        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    protected void registerUserAction(Permission permission, Id<?> entityId) {
        UserContext currentUser = currentUserProvider.currentUser();

        try {
            userRepository.save(new AuditEvent(currentUser.userId(), permission, entityId));
        } catch (DomainException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected Role getUserRole() {
        String role = currentUserProvider.currentUser().role();

        if (role == null || role.isBlank()) {
            throw new IllegalStateException("El usuario autenticado no tiene un rol.");
        }

        String normalizedRole = role.startsWith(SPRING_ROLE_PREFIX)
                ? role.substring(SPRING_ROLE_PREFIX.length())
                : role;

        try {
            return Role.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Rol de usuario no reconocido: " + role, e);
        }
    }

    protected UserContext getCurrentUser() {
        return currentUserProvider.currentUser();
    }
}
