package com.minerva.infrastructure.persistence.entity;

import com.minerva.domain.entities.user.AccountApprovalStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dni", nullable = false, unique = true)
    private PersonalEntity personal;

    @Column(name = "password", length = 60, nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_app_user_customer"))
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "approval_status", nullable = false, columnDefinition = "account_approval_status")
    private AccountApprovalStatus approvalStatus;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", foreignKey = @ForeignKey(name = "fk_app_user_approved_by"))
    private AppUserEntity approvedBy;

    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by", foreignKey = @ForeignKey(name = "fk_app_user_rejected_by"))
    private AppUserEntity rejectedBy;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;
}
