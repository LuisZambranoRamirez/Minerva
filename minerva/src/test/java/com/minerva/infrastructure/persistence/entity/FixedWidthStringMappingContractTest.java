package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedWidthStringMappingContractTest {

    @Test
    void fixedWidthStringColumnsUseTheHibernateCharJdbcType() throws NoSuchFieldException {
        assertCharColumn(PersonalEntity.class, "dni", "dni", 8, true, false);
        assertCharColumn(PersonalEntity.class, "phoneNumber", "phone_number", 9, false, true);
        assertCharColumn(SupplierEntity.class, "ruc", "ruc", 11, true, true);
        assertCharColumn(SupplierEntity.class, "phoneNumber", "phone_number", 9, true, true);
        assertCharColumn(CustomerEntity.class, "phoneNumber", "phone_number", 9, true, true);
        assertCharColumn(CustomerEntity.class, "ruc", "ruc", 11, true, false);
        assertCharColumn(ProductEntity.class, "barCode", "bar_code", 13, true, true);
    }

    @Test
    void appUserDniJoinUsesTheCharMappedPersonalIdentifier() throws NoSuchFieldException {
        Field personal = AppUserEntity.class.getDeclaredField("personal");
        JoinColumn joinColumn = personal.getAnnotation(JoinColumn.class);

        assertNotNull(joinColumn);
        assertEquals("dni", joinColumn.name());
        assertFalse(joinColumn.nullable());
        assertTrue(joinColumn.unique());
        assertEquals(PersonalEntity.class, personal.getType());

        assertNotNull(PersonalEntity.class.getDeclaredField("dni").getAnnotation(Id.class));
        assertCharColumn(PersonalEntity.class, "dni", "dni", 8, true, false);
    }

    private void assertCharColumn(
            Class<?> entityType,
            String fieldName,
            String columnName,
            int length,
            boolean nullable,
            boolean unique
    ) throws NoSuchFieldException {
        Field field = entityType.getDeclaredField(fieldName);
        JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
        Column column = field.getAnnotation(Column.class);

        assertEquals(String.class, field.getType());
        assertNotNull(jdbcTypeCode, entityType.getSimpleName() + "." + fieldName + " must map as CHAR");
        assertEquals(SqlTypes.CHAR, jdbcTypeCode.value());
        assertNotNull(column);
        assertEquals(columnName, column.name());
        assertEquals(length, column.length());
        assertEquals(nullable, column.nullable());
        assertEquals(unique, column.unique());
    }
}
