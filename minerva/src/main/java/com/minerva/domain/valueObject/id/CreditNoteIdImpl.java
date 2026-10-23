package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.sale.CreditNoteId;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.ValueObject;
import java.util.Set;
import java.util.UUID;

public final class CreditNoteIdImpl extends ValueObject<UUID> implements CreditNoteId {
    public CreditNoteIdImpl(UUID value) throws NullValueException { super(value); }
    public static CreditNoteIdImpl generate() {
        try { return new CreditNoteIdImpl(UUID.randomUUID()); }
        catch (NullValueException e) { throw new UnexpectedDomainException("No se pudo generar el ID de nota de crédito.", e); }
    }
    public static CreditNoteIdImpl fromString(String value) throws NullValueException {
        if (value == null || value.isBlank()) throw new NullValueException("El ID de nota de crédito no puede estar vacío.");
        try { return new CreditNoteIdImpl(UUID.fromString(value)); }
        catch (IllegalArgumentException e) { throw new NullValueException("El ID de nota de crédito no es un UUID válido."); }
    }
    public UUID getIdValue() { return getValue(); }
    public String getIdValueAsString() { return getValue().toString(); }
    public String getIdName() { return "creditNoteId"; }
    public String getAuditSubjectName() { return "creditNote"; }
    public Id<?> getAuditSubjectId() { return this; }
    public Set<Attribute<?>> getAuditData() { return Set.of(new StringAttribute(this)); }
}
