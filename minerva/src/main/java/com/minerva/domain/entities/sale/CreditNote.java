package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.NumericAttribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.NullValueException;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.CreditNoteIdImpl;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CreditNote extends Entity<CreditNoteId> {
    public record Line(ProductReturn productReturn, Money unitPrice) {
        public Line {
            if (productReturn == null || unitPrice == null) throw new IllegalArgumentException("La línea de crédito es obligatoria.");
        }
        public Money subtotal() { return unitPrice.multiply(productReturn.getQuantity().getValue()); }
    }

    private final SaleId saleId;
    private final String reference;
    private final LocalDateTime issuedAt;
    private final UserId issuedBy;
    private final List<Line> lines;

    public CreditNote(SaleId saleId, String reference, UserId issuedBy, List<Line> lines) throws DomainException {
        this(CreditNoteIdImpl.generate(), saleId, reference, LocalDateTime.now(), issuedBy, lines);
    }
    public CreditNote(CreditNoteId id, SaleId saleId, String reference, LocalDateTime issuedAt, UserId issuedBy, List<Line> lines) throws DomainException {
        super(id);
        if (saleId == null || issuedAt == null) throw new NullValueException("Venta y fecha de emisión son obligatorias.");
        if (reference == null || reference.isBlank()) throw new NullValueException("La referencia es obligatoria.");
        if (lines == null || lines.isEmpty()) throw new NullValueException("La nota requiere al menos una línea.");
        this.saleId = saleId; this.reference = reference.trim(); this.issuedAt = issuedAt;
        this.issuedBy = issuedBy; this.lines = List.copyOf(lines);
    }
    public Money total() { return lines.stream().map(Line::subtotal).reduce(Money.zero(), Money::add); }
    public SaleId getSaleId() { return saleId; }
    public String getReference() { return reference; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public UserId getIssuedBy() { return issuedBy; }
    public List<Line> getLines() { return lines; }
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> values = new HashSet<>(); values.add(new StringAttribute(getId()));
        values.add(new StringAttribute("reference", reference)); values.add(new NumericAttribute("total", total()));
        return Set.copyOf(values);
    }
}
