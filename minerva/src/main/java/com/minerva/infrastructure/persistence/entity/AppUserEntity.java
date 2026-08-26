package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserEntity {

    @Id
    @Column(name = "user_name", length = 30)
    private String userName;

    @Column(name = "dni", length = 8, unique = true, nullable = false)
    private String dni;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}