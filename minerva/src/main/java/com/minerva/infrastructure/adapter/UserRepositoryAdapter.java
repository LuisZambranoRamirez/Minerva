package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import com.minerva.infrastructure.persistence.entity.AppUserEntity;
import com.minerva.infrastructure.persistence.entity.AuditEventEntity;
import com.minerva.infrastructure.persistence.entity.CustomerEntity;
import com.minerva.infrastructure.persistence.entity.PersonalEntity;
import com.minerva.infrastructure.persistence.repository.JpaAppUserRepository;
import com.minerva.infrastructure.persistence.repository.JpaPersonalRepository;
import com.minerva.infrastructure.persistence.repository.JpaUserActionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JpaAppUserRepository jpaAppUserRepository;
    private final JpaPersonalRepository jpaPersonalRepository;
    private final JpaUserActionRepository jpaUserActionRepository;

    public UserRepositoryAdapter(
            JpaAppUserRepository jpaAppUserRepository,
            JpaPersonalRepository jpaPersonalRepository,
            JpaUserActionRepository jpaUserActionRepository
    ) {
        this.jpaAppUserRepository = jpaAppUserRepository;
        this.jpaPersonalRepository = jpaPersonalRepository;
        this.jpaUserActionRepository = jpaUserActionRepository;
    }

    @Override
    @Transactional
    public void save(User user) {
        PersonalEntity personalEntity = toEntity(user.getPersonal());
        jpaPersonalRepository.saveAndFlush(personalEntity);
        jpaAppUserRepository.saveAndFlush(toEntity(user, personalEntity));
    }

    @Override
    public void save(AuditEvent auditEvent) {
        jpaUserActionRepository.save(toEntity(auditEvent));
    }

    @Override
    public boolean existsById(UserId id) {
        return jpaAppUserRepository.existsById(id.getIdValueAsString());
    }

    @Override
    public boolean existsByDNI(DNI dni) {
        return jpaAppUserRepository.existsByPersonal_Dni(dni.getValue());
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return jpaPersonalRepository.existsByPhoneNumber(phoneNumber.getValue());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaPersonalRepository.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByCustomerId(com.minerva.domain.entities.customer.CustomerId customerId) {
        return jpaAppUserRepository.existsByCustomer_CustomerId(customerId.getIdValue());
    }

    @Override
    public boolean hasUsers() {
        return jpaAppUserRepository.count() > 0;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaAppUserRepository.findById(id.getIdValueAsString()).map(this::toDomain);
    }

    @Override
    public List<User> findClienteAccounts(AccountApprovalStatus status) {
        if (status == null) {
            return jpaAppUserRepository.findByPersonal_RoleOrderByRegistrationDateAsc(Role.CLIENTE)
                    .stream()
                    .map(this::toDomain)
                    .toList();
        }
        return jpaAppUserRepository.findByPersonal_RoleAndApprovalStatusOrderByRegistrationDateAsc(Role.CLIENTE, status)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private User toDomain(AppUserEntity appUserEntity) {
        PersonalEntity personalEntity = appUserEntity.getPersonal();

        try {
            Personal personal = new Personal(
                    personalEntity.getDni(),
                    personalEntity.getNames(),
                    personalEntity.getLastNames(),
                    personalEntity.getPhoneNumber(),
                    personalEntity.getRole(),
                    personalEntity.getEmail(),
                    Boolean.TRUE.equals(personalEntity.getActive()),
                    personalEntity.getRegistrationDate()
            );

            return new User(
                    personal,
                    appUserEntity.getUserName(),
                    appUserEntity.getPassword(),
                    Boolean.TRUE.equals(appUserEntity.getActive()),
                    appUserEntity.getRegistrationDate(),
                    appUserEntity.getCustomer() == null ? null : new CustomerIdImpl(appUserEntity.getCustomer().getCustomerId()),
                    appUserEntity.getApprovalStatus() == null ? AccountApprovalStatus.APPROVED : appUserEntity.getApprovalStatus(),
                    appUserEntity.getApprovedDate(),
                    appUserEntity.getApprovedBy() == null ? null : new UserName(appUserEntity.getApprovedBy().getUserName()),
                    appUserEntity.getRejectedDate(),
                    appUserEntity.getRejectedBy() == null ? null : new UserName(appUserEntity.getRejectedBy().getUserName()),
                    appUserEntity.getRejectionReason()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el usuario.", e);
        }
    }

    private PersonalEntity toEntity(Personal personal) {
        return new PersonalEntity(
                personal.getId().getValue(),
                personal.getNames().getValue(),
                personal.getLastNames().getValue(),
                personal.getPhoneNumber().getValue(),
                personal.getRole(),
                personal.getEmail().getValue(),
                personal.isActive(),
                personal.getRegistrationDate()
        );
    }

    private AppUserEntity toEntity(User user, PersonalEntity personalEntity) {
        CustomerEntity customerEntity = user.getCustomerId() == null
                ? null
                : entityManager.getReference(CustomerEntity.class, user.getCustomerId().getIdValue());
        AppUserEntity approvedBy = user.getApprovedBy() == null
                ? null
                : entityManager.getReference(AppUserEntity.class, user.getApprovedBy().getIdValueAsString());
        AppUserEntity rejectedBy = user.getRejectedBy() == null
                ? null
                : entityManager.getReference(AppUserEntity.class, user.getRejectedBy().getIdValueAsString());

        return AppUserEntity.builder()
                .userName(user.getUsername().getValue())
                .personal(personalEntity)
                .password(user.getPasswordHash().getValue())
                .active(user.isActive())
                .registrationDate(user.getRegistrationDate())
                .customer(customerEntity)
                .approvalStatus(user.getApprovalStatus())
                .approvedDate(user.getApprovedDate())
                .approvedBy(approvedBy)
                .rejectedDate(user.getRejectedDate())
                .rejectedBy(rejectedBy)
                .rejectionReason(user.getRejectionReason())
                .build();
    }

    private AuditEventEntity toEntity(AuditEvent auditEvent) {
        AppUserEntity appUserEntity = entityManager.getReference(
                AppUserEntity.class,
                auditEvent.getUserId().getIdValueAsString()
        );

        return new AuditEventEntity(
                auditEvent.getId().getIdValue(),
                appUserEntity,
                auditEvent.getEventType(),
                auditEvent.getPermission(),
                auditEvent.getAuditable().getAuditSubjectId().getIdValueAsString(),
                auditEvent.getAuditable().getAuditSubjectName(),
                toAttributeMap(auditEvent.getAuditable().getAuditData()),
                auditEvent.getRegistrationDate()
        );
    }

    private Map<String, Object> toAttributeMap(Iterable<Attribute<?>> attributes) {
        Map<String, Object> data = new LinkedHashMap<>();

        for (Attribute<?> attribute : attributes) {
            data.put(attribute.getName(), attribute.getAttributeValue().orElse(null));
        }

        return data;
    }
}
