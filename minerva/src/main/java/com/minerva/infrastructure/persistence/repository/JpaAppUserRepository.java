package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.AppUserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAppUserRepository extends JpaRepository<AppUserEntity, String> {
    boolean existsByDNI(String dni);
}
