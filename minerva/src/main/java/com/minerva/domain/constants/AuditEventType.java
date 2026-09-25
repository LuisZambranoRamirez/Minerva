package com.minerva.domain.constants;

public enum AuditEventType {
    CREATE,
    READ,
    UPDATE,
    DELETE;

    public static AuditEventType fromPermission(Permission permission) {
        String name = permission.name();

        if (name.contains("_UPDATE_")) return UPDATE;
        if (name.contains("_FIND_") || name.endsWith("_FIND_ALL")
                || name.endsWith("_GET_ALL") || name.endsWith("_AUTHENTICATE")) {
            return READ;
        }

        return CREATE;
    }
}
