package com.minerva.domain.entities.user;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.*;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.valueObject.*;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.id.UserName;

public class User extends Entity<UserId> implements UserReader {
    private final DNI dni;
    private FullName fullName;
    private final UserName username;
    private PasswordHash passwordHash;
    private Role role;
    private boolean isActive;
    private final LocalDateTime registrationDate;

    public User(PasswordHasher passwordHasher, String dni, String fullName, String username, String password, Role role) throws DomainException {
        if (role == null) throw new NullValueException("El ROL no puede ser nulo.");
        UserName tempUserName = new UserName(username);
        super(tempUserName);
        this.dni = new DNI(dni);
        this.fullName = new FullName(fullName);
        this.username = tempUserName;
        this.passwordHash = passwordHasher.hash(new Password(password));
        this.role = role;
        this.isActive = true;
        this.registrationDate = LocalDateTime.now();
    }

    public User(String dni, String fullName, String username, String password, Role role, boolean isActive, LocalDateTime registrationDate) {
        UserName tempUserName;
        try {
            tempUserName = new UserName(username);
            this.dni = new DNI(dni);
            this.username = tempUserName;
            this.fullName = new FullName(fullName);
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
    public FullName getFullName() {
        return fullName;
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
    public Map<String, Attribute<?>> extractAuditData() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "userId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "dni",
                dni
        );

        attributes.put(
                "fullName",
                fullName
        );

        attributes.put(
                "username",
                username
        );

        attributes.put(
                "passwordHash",
                passwordHash
        );

        attributes.put(
                "role",
                role
        );

        attributes.put(
                "isActive",
                new DefaultStringAttribute(String.valueOf(isActive))
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
