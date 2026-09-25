package com.minerva.domain.entities.auditEvent;

import java.time.LocalDateTime;
import java.util.Set;

import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.AuditEventType;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.id.AuditEventIdImpl;
import com.minerva.domain.valueObject.id.UserName;

public class AuditEvent extends Entity<AuditEventId> {
    private final UserId userId;
    private final AuditEventType eventType;
    private final Permission permission;
    private final Auditable auditable;
    
    private final LocalDateTime registrationDate;

    public AuditEvent(String userName, Permission permission, Auditable auditable) throws InvalidDomainArgumentException {
        super(AuditEventIdImpl.generate());

        if (permission == null) throw new NullValueException("El permiso no puede ser nulo.");
        if (auditable == null) throw new NullValueException("La entidad no puede ser nula.");

        this.eventType = AuditEventType.fromPermission(permission);
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

    public AuditEventType getEventType() {
        return eventType;
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
            new StringAttribute(eventType),
            new StringAttribute(permission),
            new StringAttribute("subjectId", auditable.getAuditSubjectId().getIdValueAsString()),
            new StringAttribute("subjectName", auditable.getAuditSubjectName()),
            new StringAttribute("subjectData", auditable.getAuditData().toString()),
            new StringAttribute("registrationDate", registrationDate)
        );
    }
}
