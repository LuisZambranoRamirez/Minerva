package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.DNI;
import com.minerva.infrastructure.persistence.entity.AuditEventEntity;
import com.minerva.infrastructure.persistence.entity.AppUserEntity;
import com.minerva.infrastructure.persistence.repository.JpaUserActionRepository;
import com.minerva.infrastructure.persistence.repository.JpaAppUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JpaAppUserRepository jpaAppUserRepository;
    private final JpaUserActionRepository jpaUserActionRepository;

    public UserRepositoryAdapter(JpaAppUserRepository jpaAppUserRepository, JpaUserActionRepository jpaUserActionRepository) {
        this.jpaAppUserRepository = jpaAppUserRepository;
        this.jpaUserActionRepository = jpaUserActionRepository;
    }

    @Override
    public void save(User user) {
        jpaAppUserRepository.save(toEntity(user));
    }

    @Override
    public void save(AuditEvent auditEvent) {
        jpaUserActionRepository.save(toEntity(auditEvent));
    }

    @Override
    public boolean existsById(UserId dni) {
        return jpaAppUserRepository.existsById(dni.value());
    }

    @Override
    public boolean existsByDNI(DNI dni) {
        return jpaAppUserRepository.existsByDNI(dni.value);
    }

    @Override
    public Optional<User> findById(UserId dni) {
        return jpaAppUserRepository.findById(dni.value()).map(this::toDomain);
    }

    private User toDomain(AppUserEntity appUserEntity) {
        return new User(
                appUserEntity.getDni(),
                appUserEntity.getNames(),
                appUserEntity.getLastNames(),
                appUserEntity.getUsername(),
                appUserEntity.getPassword(),
                appUserEntity.getRole(),
                appUserEntity.isActive(),
                appUserEntity.getRegistrationDate()
        );
    }

    private AppUserEntity toEntity(User user) {
        return new AppUserEntity(
                user.getUsername().value,
                user.getDni().value,
                user.getNames().value,
                user.getLastNames().value,
                user.getPasswordHash().value,
                user.getRole(),
                user.isActive(),
                user.getRegistrationDate()
        );
    }

    private AuditEventEntity toEntity(AuditEvent auditEvent) {
        AppUserEntity appUserEntity = entityManager.getReference(AppUserEntity.class, auditEvent.getUserName());


        return new AuditEventEntity(
                auditEvent.getId().getIdValueAsString(),
                appUserEntity,
                auditEvent.getPermission(),
                auditEvent.getEntityId().asString(),
                auditEvent.getRegistrationDate()
        );
    }
}
