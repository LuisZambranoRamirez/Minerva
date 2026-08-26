package com.minerva.domain.entities.auditEvent;

import java.time.LocalDateTime;
import java.util.Set;

import com.minerva.domain.constants.Permission;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.id.AuditEventIdImpl;
import com.minerva.domain.valueObject.id.UserName;

public class AuditEvent extends Entity<AuditEventId> {
    private final UserId userId;
    private final Permission permission;
    private final Auditable auditable;
    
    private final LocalDateTime registrationDate;

    public AuditEvent(String userName, Permission permission, Auditable auditable) throws InvalidDomainArgumentException {
        if (permission == null) throw new NullValueException("El permiso no puede ser nulo.");
        if (auditable == null) throw new NullValueException("La entidad no puede ser nula.");

        super(AuditEventIdImpl.generate());
        this.permission = permission;
        this.userId = new UserName(userName);
        this.auditable = auditable;
        this.registrationDate = LocalDateTime.now();
    }

    public UserId getUserId() {
        return userId;
    }

    public Permission getPermission() {
        return permission;
    }

    public Auditable getAuditable() {
        return auditable;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
            new StringAttribute(getId()),
            new StringAttribute(userId),
            new StringAttribute(permission),
            new StringAttribute(auditable.getAuditSubjectId()),
            new StringAttribute("subject_name", auditable.getAuditSubjectName()),
            new StringAttribute("subject_data", auditable.getAuditData().toString()),
            new StringAttribute("registration_date", registrationDate)
        );
    }
}
