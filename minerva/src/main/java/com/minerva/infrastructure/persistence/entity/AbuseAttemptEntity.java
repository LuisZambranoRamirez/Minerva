package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abuse_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbuseAttemptEntity {
    @Id
    @Column(name = "abuse_attempt_id", nullable = false)
    private UUID abuseAttemptId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "subject_key", nullable = false, length = 120)
    private String subjectKey;

    @Column(name = "successful", nullable = false)
    private Boolean successful;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}