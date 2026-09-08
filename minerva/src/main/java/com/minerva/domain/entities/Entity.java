package com.minerva.domain.entities;

import com.minerva.domain.entities.auditEvent.Auditable;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.id.Id;

import java.util.Objects;

public abstract class Entity<I extends Id<?>> implements Auditable {
    private final I id;

    public Entity(I id) {
        if (id == null) throw new UnexpectedDomainException("El ID no puede ser nulo");
        this.id = id;
    }

    @Override
    public String getAuditSubjectName() {
        return getClass().getSimpleName();
    }

    @Override
    public Id<?> getAuditSubjectId() {
        return id;
    }

    public I getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Entity<?> entity)) return false;
        return Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
