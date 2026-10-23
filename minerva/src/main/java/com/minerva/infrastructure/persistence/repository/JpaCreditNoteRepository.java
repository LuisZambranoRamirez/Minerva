package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.CreditNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaCreditNoteRepository extends JpaRepository<CreditNoteEntity, UUID> {
    @Query("""
            select note from CreditNoteEntity note
            join fetch note.sale sale
            left join fetch note.issuedBy
            where sale.saleId = :saleId
            order by note.issuedAt asc
            """)
    List<CreditNoteEntity> findReadModelBySaleId(@Param("saleId") UUID saleId);

    @Query("""
            select note from CreditNoteEntity note
            join fetch note.sale sale
            left join fetch note.issuedBy
            where sale.saleId in :saleIds
            order by sale.saleId, note.issuedAt asc
            """)
    List<CreditNoteEntity> findReadModelBySaleIds(@Param("saleIds") Collection<UUID> saleIds);
}
