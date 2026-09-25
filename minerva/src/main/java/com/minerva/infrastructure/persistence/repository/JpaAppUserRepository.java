package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.infrastructure.persistence.entity.AppUserEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAppUserRepository extends JpaRepository<AppUserEntity, String> {
    @Override
    @EntityGraph(attributePaths = "personal")
    Optional<AppUserEntity> findById(String userName);

    boolean existsByPersonal_Dni(String dni);
    boolean existsByCustomer_CustomerId(UUID customerId);
    Optional<AppUserEntity> findByCustomer_CustomerId(UUID customerId);
    List<AppUserEntity> findByApprovalStatusOrderByRegistrationDateAsc(AccountApprovalStatus approvalStatus);

    @EntityGraph(attributePaths = "personal")
    List<AppUserEntity> findByPersonal_RoleOrderByRegistrationDateAsc(Role role);

    @EntityGraph(attributePaths = "personal")
    List<AppUserEntity> findByPersonal_RoleAndApprovalStatusOrderByRegistrationDateAsc(Role role, AccountApprovalStatus approvalStatus);
}
