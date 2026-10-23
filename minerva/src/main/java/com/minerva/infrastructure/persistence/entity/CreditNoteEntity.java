package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "credit_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteEntity {
    @Id @Column(name = "credit_note_id", nullable = false) private UUID creditNoteId;
    @Column(name = "reference", nullable = false, unique = true, length = 64) private String reference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sale_id", nullable = false) private SaleEntity sale;
    @Column(name = "issued_at", nullable = false) private LocalDateTime issuedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issued_by") private AppUserEntity issuedBy;
}
