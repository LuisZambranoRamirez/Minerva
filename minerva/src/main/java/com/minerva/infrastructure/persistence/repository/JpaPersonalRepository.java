package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.PersonalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPersonalRepository extends JpaRepository<PersonalEntity, String> {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
