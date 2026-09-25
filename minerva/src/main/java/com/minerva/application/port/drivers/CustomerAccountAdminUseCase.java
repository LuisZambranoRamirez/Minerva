package com.minerva.application.port.drivers;

import com.minerva.domain.entities.user.AccountApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerAccountAdminUseCase {
    List<CustomerAccountSummary> list(AccountApprovalStatus status);
    CustomerAccountSummary approve(String username);
    CustomerAccountSummary reject(String username, String reason);

    record CustomerAccountSummary(
            String username,
            String customerId,
            String businessName,
            String legalName,
            String ruc,
            String contactName,
            String contactPhone,
            String contactEmail,
            String approvalStatus,
            LocalDateTime registrationDate,
            LocalDateTime approvedDate,
            String approvedBy,
            LocalDateTime rejectedDate,
            String rejectedBy,
            String rejectionReason
    ) {}
}