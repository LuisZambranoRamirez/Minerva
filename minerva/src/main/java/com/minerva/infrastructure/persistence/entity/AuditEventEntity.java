package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.AuditEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventEntity {

    @Id
    @Column(name = "audit_event_id", nullable = false)
    private UUID auditEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_name",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_event_user")
    )
    private AppUserEntity user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "permission", nullable = false)
    private Permission permission;

    @Column(name = "subject_id", nullable = false)
    private String subjectId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "subject_data",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private Map<String, Object> subjectData;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}
