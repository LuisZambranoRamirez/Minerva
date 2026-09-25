package com.minerva.domain.entities.user;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.BooleanAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Password;
import com.minerva.domain.valueObject.PasswordHash;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.valueObject.id.UserName;

import java.time.LocalDateTime;
import java.util.Set;

public class User extends Entity<UserId> implements UserReader {

    private final Personal personal;
    private final UserName username;
    private final PasswordHash passwordHash;
    private final boolean active;
    private final LocalDateTime registrationDate;
    private final CustomerId customerId;
    private final AccountApprovalStatus approvalStatus;
    private final LocalDateTime approvedDate;
    private final UserId approvedBy;
    private final LocalDateTime rejectedDate;
    private final UserId rejectedBy;
    private final String rejectionReason;

    public User(PasswordHasher passwordHasher, Personal personal, String username, String password) throws DomainException {
        this(
                personal,
                new UserName(username),
                passwordHasher.hash(new Password(password)),
                true,
                LocalDateTime.now(),
                null,
                AccountApprovalStatus.APPROVED,
                null,
                null,
                null,
                null,
                null
        );
    }

    public User(Personal personal, String username, String passwordHash, boolean active, LocalDateTime registrationDate) throws DomainException {
        this(
                personal,
                new UserName(username),
                new PasswordHash(passwordHash),
                active,
                registrationDate,
                null,
                AccountApprovalStatus.APPROVED,
                null,
                null,
                null,
                null,
                null
        );
    }

    public User(
            Personal personal,
            String username,
            String passwordHash,
            boolean active,
            LocalDateTime registrationDate,
            CustomerId customerId,
            AccountApprovalStatus approvalStatus,
            LocalDateTime approvedDate,
            UserId approvedBy,
            LocalDateTime rejectedDate,
            UserId rejectedBy,
            String rejectionReason
    ) throws DomainException {
        this(
                personal,
                new UserName(username),
                new PasswordHash(passwordHash),
                active,
                registrationDate,
                customerId,
                approvalStatus,
                approvedDate,
                approvedBy,
                rejectedDate,
                rejectedBy,
                rejectionReason
        );
    }

    private User(
            Personal personal,
            UserName username,
            PasswordHash passwordHash,
            boolean active,
            LocalDateTime registrationDate,
            CustomerId customerId,
            AccountApprovalStatus approvalStatus,
            LocalDateTime approvedDate,
            UserId approvedBy,
            LocalDateTime rejectedDate,
            UserId rejectedBy,
            String rejectionReason
    ) throws NullValueException {
        super(username);

        if (personal == null) {
            throw new NullValueException("Los datos personales del usuario no pueden ser nulos.");
        }
        if (registrationDate == null) {
            throw new NullValueException("La fecha de registro del usuario no puede ser nula.");
        }

        this.personal = personal;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.registrationDate = registrationDate;
        this.customerId = customerId;
        this.approvalStatus = approvalStatus == null ? AccountApprovalStatus.APPROVED : approvalStatus;
        this.approvedDate = approvedDate;
        this.approvedBy = approvedBy;
        this.rejectedDate = rejectedDate;
        this.rejectedBy = rejectedBy;
        this.rejectionReason = normalizeNullable(rejectionReason);
    }

    public boolean authenticate(String password, PasswordHasher passwordHasher) {
        if (!canAuthenticate()) {
            return false;
        }

        return passwordHasher.matches(password, passwordHash);
    }

    public boolean canAuthenticate() {
        return active && personal.isActive() && approvalStatus.allowsLogin();
    }

    public boolean isPendingApproval() {
        return approvalStatus == AccountApprovalStatus.PENDING_APPROVAL;
    }

    public boolean isApproved() {
        return approvalStatus == AccountApprovalStatus.APPROVED;
    }

    public boolean isRejected() {
        return approvalStatus == AccountApprovalStatus.REJECTED;
    }


    public User approve(UserId approverId) throws DomainException {
        if (!isPendingApproval()) {
            throw new DomainException("Solo se pueden aprobar cuentas pendientes.");
        }
        if (approverId == null) {
            throw new NullValueException("El aprobador no puede ser nulo.");
        }
        return new User(
                personal,
                username,
                passwordHash,
                active,
                registrationDate,
                customerId,
                AccountApprovalStatus.APPROVED,
                LocalDateTime.now(),
                approverId,
                null,
                null,
                null
        );
    }

    public User reject(UserId rejecterId, String reason) throws DomainException {
        if (!isPendingApproval()) {
            throw new DomainException("Solo se pueden rechazar cuentas pendientes.");
        }
        if (rejecterId == null) {
            throw new NullValueException("El rechazador no puede ser nulo.");
        }
        String normalizedReason = normalizeNullable(reason);
        if (normalizedReason == null) {
            throw new NullValueException("El motivo de rechazo no puede ser nulo.");
        }
        if (normalizedReason.length() > 255) {
            normalizedReason = normalizedReason.substring(0, 255);
        }
        return new User(
                personal,
                username,
                passwordHash,
                active,
                registrationDate,
                customerId,
                AccountApprovalStatus.REJECTED,
                null,
                null,
                LocalDateTime.now(),
                rejecterId,
                normalizedReason
        );
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Personal getPersonal() {
        return personal;
    }

    @Override
    public DNI getDni() {
        return personal.getId();
    }

    @Override
    public UserName getUsername() {
        return username;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    @Override
    public Role getRole() {
        return personal.getRole();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }
    public CustomerId getCustomerId() {
        return customerId;
    }

    public AccountApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public UserId getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getRejectedDate() {
        return rejectedDate;
    }

    public UserId getRejectedBy() {
        return rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }


    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute((Id<?>) getId()),
                new StringAttribute((Id<?>) personal.getId()),
                new StringAttribute(getRole()),
                new BooleanAttribute("isActive", active),
                new StringAttribute("registrationDate", registrationDate),
                new StringAttribute("approvalStatus", approvalStatus.name())
        );
    }
}
