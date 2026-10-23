package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CreditNoteReadModelStaticTest {

    @Test
    void saleListLoadsCreditNotesAndLinesInBulkInsteadOfPerSaleAndPerNote() throws Exception {
        String adapter = Files.readString(Path.of(
                "src/main/java/com/minerva/infrastructure/adapter/SaleRepositoryAdapter.java"));
        String notes = Files.readString(Path.of(
                "src/main/java/com/minerva/infrastructure/persistence/repository/JpaCreditNoteRepository.java"));
        String lines = Files.readString(Path.of(
                "src/main/java/com/minerva/infrastructure/persistence/repository/JpaProductReturnRepository.java"));

        assertThat(adapter).contains("loadCreditNotesBySaleIds", "findReadModelBySaleIds",
                "findReadModelLinesByCreditNoteIds");
        assertThat(adapter).doesNotContain(".map(this::toCreditNoteDomain)");
        assertThat(notes).contains("where sale.saleId in :saleIds", "left join fetch note.issuedBy");
        assertThat(lines).contains("where note.creditNoteId in :creditNoteIds", "join fetch productReturn.saleDetail");
    }
}
