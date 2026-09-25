package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalEntity {

    @Id
    @Column(name = "dni", length = 8)
    private String dni;

    @Column(name = "names", length = 100, nullable = false)
    private String names;

    @Column(name = "lastnames", length = 100, nullable = false)
    private String lastNames;

    @Column(name = "phone_number", length = 9, nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role_name", nullable = false)
    private Role role;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
