package com.minerva.domain.entities.userAction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.minerva.domain.constants.Permission;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.id.Id;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.id.UserActionIdImpl;
import com.minerva.domain.valueObject.id.UserName;

public class UserAction extends Entity<UserActionId> {
    private final UserId userId;
    private final Permission permission;
    private final Entity<?> entity;
    
    private final LocalDateTime registrationDate;

    public UserAction(String userName, Permission permission, Entity<?> entity) throws InvalidDomainArgumentException {
        if (permission == null) throw new NullValueException("El permiso no puede ser nulo.");
        if (entity == null) throw new NullValueException("La entidad no puede ser nula.");

        super(UserActionIdImpl.generate());
        this.permission = permission;
        this.userId = new UserName(userName);
        this.entity = entity;
        this.registrationDate = LocalDateTime.now();
    }

    public UserId getUserId() {
        return userId;
    }

    public String getEntityName() {
        return entity.getEntityName();
    }

    public Permission getPermission() {
        return permission;
    }

    public Id<?> getEntityId() {
        return entity.getId();
    }

    public Map<String, Attribute<?>> getEntityData() {
        return entity.getAttributes();
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Map<String, Attribute<?>> getAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "userId",
                new DefaultStringAttribute(userId.asString())
        );

        attributes.put(
                "permission",
                permission
        );

        attributes.put(
                "entityName",
                new DefaultStringAttribute(entity.getEntityName())
        );

        attributes.put(
                "entityId",
                new DefaultStringAttribute(entity.getId().asString())
        );

        attributes.put(
                "entityData",
                new DefaultStringAttribute(entity.getAttributes().toString())
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
