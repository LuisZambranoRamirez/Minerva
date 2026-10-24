package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageMigrationStaticTest {
    @Test
    void v11IsNullableSchemaOnly() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V11__add_product_image_path.sql")).toLowerCase();
        assertTrue(sql.contains("image_path varchar(120) null"));
        assertFalse(sql.contains("insert"));
    }
}
