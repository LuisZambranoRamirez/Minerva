package com.minerva.infrastructure.persistence.repository;

import com.minerva.infrastructure.persistence.entity.ProductReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaProductReturnRepository extends JpaRepository<ProductReturnEntity, UUID> {

    List<ProductReturnEntity> findBySaleDetail_SaleDetailId(UUID saleDetailId);
    List<ProductReturnEntity> findByCreditNote_CreditNoteId(UUID creditNoteId);

    @Query("""
            select productReturn from ProductReturnEntity productReturn
            join fetch productReturn.creditNote note
            join fetch productReturn.saleDetail detail
            where note.creditNoteId in :creditNoteIds
            order by note.issuedAt, productReturn.registrationDate, productReturn.productReturnId
            """)
    List<ProductReturnEntity> findReadModelLinesByCreditNoteIds(
            @Param("creditNoteIds") Collection<UUID> creditNoteIds);
}

