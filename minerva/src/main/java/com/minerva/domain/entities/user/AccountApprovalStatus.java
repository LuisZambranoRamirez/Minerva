package com.minerva.domain.entities.user;

public enum AccountApprovalStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED;

    public boolean allowsLogin() {
        return this == APPROVED;
    }
}
