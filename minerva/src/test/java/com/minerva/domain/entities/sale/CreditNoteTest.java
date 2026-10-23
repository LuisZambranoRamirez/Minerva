package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;
import com.minerva.domain.valueObject.id.SaleIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditNoteTest {
    @Test
    void totalsMultipleLinesWithTheCanonicalMoneyRounding() throws Exception {
        ProductReturn first = new ProductReturn(new SaleDetailIdImpl(UUID.randomUUID()),
                new ProductQuantity(new BigDecimal("1.500")), ProductReturnReason.EQUIVOCACION);
        ProductReturn second = new ProductReturn(new SaleDetailIdImpl(UUID.randomUUID()),
                new ProductQuantity(new BigDecimal("2.000")), ProductReturnReason.DAÑADO);
        CreditNote note = new CreditNote(new SaleIdImpl(UUID.randomUUID()), "NC-TEST",
                new UserName("admin"), List.of(
                new CreditNote.Line(first, new Money(new BigDecimal("10.00"))),
                new CreditNote.Line(second, new Money(new BigDecimal("2.55")))));
        assertThat(note.total().getValue()).isEqualByComparingTo("20.10");
        assertThat(note.getLines()).hasSize(2);
    }

    @Test
    void roundsEveryLineBeforeAddingTheCreditNoteTotal() throws Exception {
        CreditNote note = noteWithPricesAndQuantities("0.01", "0.500", "0.01", "0.500");

        assertThat(note.getLines()).extracting(line -> line.subtotal().getValue().toPlainString())
                .containsExactly("0.01", "0.01");
        assertThat(note.total().getValue()).isEqualByComparingTo("0.02");
    }

    @Test
    void rejectsEmptyLinesAndKeepsItsLinesImmutable() throws Exception {
        assertThatThrownBy(() -> new CreditNote(new SaleIdImpl(UUID.randomUUID()), "NC-EMPTY",
                new UserName("admin"), List.of())).hasMessageContaining("al menos una línea");

        CreditNote note = noteWithPrices("1.00", "2.00");
        assertThatThrownBy(() -> note.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private CreditNote noteWithPrices(String firstPrice, String secondPrice) throws Exception {
        return noteWithPricesAndQuantities(firstPrice, "1.000", secondPrice, "1.000");
    }

    private CreditNote noteWithPricesAndQuantities(String firstPrice, String firstQuantity,
                                                    String secondPrice, String secondQuantity) throws Exception {
        ProductReturn first = new ProductReturn(new SaleDetailIdImpl(UUID.randomUUID()),
                new ProductQuantity(new BigDecimal(firstQuantity)), ProductReturnReason.EQUIVOCACION);
        ProductReturn second = new ProductReturn(new SaleDetailIdImpl(UUID.randomUUID()),
                new ProductQuantity(new BigDecimal(secondQuantity)), ProductReturnReason.DAÑADO);
        return new CreditNote(new SaleIdImpl(UUID.randomUUID()), "NC-MATRIX-" + UUID.randomUUID(),
                new UserName("admin"), List.of(
                new CreditNote.Line(first, new Money(new BigDecimal(firstPrice))),
                new CreditNote.Line(second, new Money(new BigDecimal(secondPrice)))));
    }
}
