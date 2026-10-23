package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class CreditNoteMigrationStaticTest {
    @Test
    void migrationBackfillsBeforeRequiringTheForeignKey() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V9__add_credit_notes.sql"));
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS credit_note", "LEGACY-", "credit_note_id SET NOT NULL");
        assertThat(sql.indexOf("UPDATE product_return")).isLessThan(sql.indexOf("credit_note_id SET NOT NULL"));
        assertThat(sql.indexOf("INSERT INTO credit_note")).isLessThan(sql.indexOf("UPDATE product_return"));
        assertThat(sql).contains("SALE_ISSUE_CREDIT_NOTE", "SALE_FIND_CREDIT_NOTES");
        assertThat(sql).contains("ON DELETE RESTRICT", "fk_product_return_credit_note",
                "idx_credit_note_sale_issued", "idx_product_return_credit_note");
    }
}
