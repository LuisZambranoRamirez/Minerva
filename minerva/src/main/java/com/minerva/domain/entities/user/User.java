package com.minerva.domain.entities.user;

import java.time.LocalDateTime;
import java.util.Set;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.BooleanAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.valueObject.*;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.UserName;

public class User extends Entity<UserId> implements UserReader {
    private final DNI dni;
    private final UserName username;
    private PasswordHash passwordHash;
    private Role role;
    private boolean isActive;
    private final LocalDateTime registrationDate;

    public User(PasswordHasher passwordHasher, String dni, String username, String password, Role role) throws DomainException {
        if (role == null) throw new NullValueException("El ROL no puede ser nulo.");
        UserName tempUserName = new UserName(username);
        super(tempUserName);
        this.dni = new DNI(dni);
        this.username = tempUserName;
        this.passwordHash = passwordHasher.hash(new Password(password));
        this.role = role;
        this.isActive = true;
        this.registrationDate = LocalDateTime.now();
    }

    public User(String dni, String username, String password, Role role, boolean isActive, LocalDateTime registrationDate) {
        UserName tempUserName;
        try {
            tempUserName = new UserName(username);
            this.dni = new DNI(dni);
            this.username = tempUserName;
            this.passwordHash = new PasswordHash(password);
            this.role = role;
            this.isActive = isActive;
            this.registrationDate = registrationDate;
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al cargar el usuario", e);
        }
        super(tempUserName);
    }

    public boolean authenticate(String password, PasswordHasher passwordHasher) {
        if (!isActive) return false;
        return passwordHasher.matches(password, passwordHash);
    }

    @Override
    public DNI getDni() {
        return dni;
    }

    @Override
    public UserName getUsername() {
        return username;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new StringAttribute(dni),
                new StringAttribute(username),
                new StringAttribute(role),
                new BooleanAttribute("isActive", isActive),
                new StringAttribute("registrationDate", registrationDate)
        );
    }
}
