package com.minerva.domain.entities.personal;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.BooleanAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.LastNames;
import com.minerva.domain.valueObject.Names;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.id.Id;

import java.time.LocalDateTime;
import java.util.Set;

public final class Personal extends Entity<DNI> {

    private final Names names;
    private final LastNames lastNames;
    private final PhoneNumber phoneNumber;
    private final Role role;
    private final Email email;
    private final boolean active;
    private final LocalDateTime registrationDate;

    public Personal(
            String dni,
            String names,
            String lastNames,
            String phoneNumber,
            Role role,
            String email
    ) throws InvalidDomainArgumentException {
        this(dni, names, lastNames, phoneNumber, role, email, true, LocalDateTime.now());
    }

    public Personal(
            String dni,
            String names,
            String lastNames,
            String phoneNumber,
            Role role,
            String email,
            boolean active,
            LocalDateTime registrationDate
    ) throws InvalidDomainArgumentException {
        super(new DNI(dni));

        if (role == null) {
            throw new NullValueException("El rol del personal no puede ser nulo.");
        }
        if (registrationDate == null) {
            throw new NullValueException("La fecha de registro del personal no puede ser nula.");
        }

        this.names = new Names(names);
        this.lastNames = new LastNames(lastNames);
        this.phoneNumber = new PhoneNumber(phoneNumber);
        this.role = role;
        this.email = new Email(email);
        this.active = active;
        this.registrationDate = registrationDate;
    }

    public Names getNames() {
        return names;
    }

    public LastNames getLastNames() {
        return lastNames;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public Role getRole() {
        return role;
    }

    public Email getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute((Id<?>) getId()),
                new StringAttribute(names),
                new StringAttribute(lastNames),
                new StringAttribute(phoneNumber),
                new StringAttribute(role),
                new StringAttribute(email),
                new BooleanAttribute("isActive", active),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
