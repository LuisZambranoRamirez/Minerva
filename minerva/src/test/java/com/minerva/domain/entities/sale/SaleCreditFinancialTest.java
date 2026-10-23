package com.minerva.domain.entities.sale;

import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.valueObject.Money;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.PayIdImpl;
import com.minerva.domain.valueObject.id.ProductIdImpl;
import com.minerva.domain.valueObject.id.SaleDetailIdImpl;
import com.minerva.domain.valueObject.id.SaleIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SaleCreditFinancialTest {

    @Test
    void partialCreditReducesDebtWithoutMutatingHistoricalPayments() throws Exception {
        Sale sale = sale("100.00", "30.00");
        sale.restoreCreditNotes(List.of(credit(sale, "20.00", "1.000")));

        assertThat(sale.calculateTotal().getValue()).isEqualByComparingTo("100.00");
        assertThat(sale.calculateTotalPaid().getValue()).isEqualByComparingTo("30.00");
        assertThat(sale.calculateTotalCredited().getValue()).isEqualByComparingTo("20.00");
        assertThat(sale.calculateAmountDue().getValue()).isEqualByComparingTo("50.00");
        assertThat(sale.calculateCreditBalance().getValue()).isEqualByComparingTo("0");
        assertThat(sale.calculateCreditStatus()).isEqualTo(Sale.CreditStatus.ACREDITADA_PARCIALMENTE);
        assertThat(sale.getPays()).hasSize(1);
    }

    @Test
    void fullCreditHasAnAdditiveFinancialStatusDistinctFromOperationalCancellation() throws Exception {
        Sale sale = sale("100.00", "0.00");
        sale.restoreCreditNotes(List.of(credit(sale, "100.00", "1.000")));

        assertThat(sale.calculateAmountDue().isZero()).isTrue();
        assertThat(sale.calculateCreditStatus()).isEqualTo(Sale.CreditStatus.ACREDITADA_TOTALMENTE);
    }

    @Test
    void creditBeyondOutstandingDebtFloorsDebtAtZeroAndExposesNonRefundedBalance() throws Exception {
        Sale sale = sale("100.00", "80.00");
        sale.restoreCreditNotes(List.of(credit(sale, "30.00", "1.000")));

        assertThat(sale.calculateAmountDue().getValue()).isEqualByComparingTo("0");
        assertThat(sale.calculateCreditBalance().getValue()).isEqualByComparingTo("10.00");
        assertThat(sale.calculateTotalPaid().getValue()).isEqualByComparingTo("80.00");
    }

    @Test
    void paymentValidationUsesNetDebtAfterCredit() throws Exception {
        Sale sale = sale("100.00", "30.00");
        sale.restoreCreditNotes(List.of(credit(sale, "20.00", "1.000")));

        assertThat(sale.addPayment(new Sale.PayCreateDTO(new BigDecimal("50.01"), PaymentMethod.EFECTIVO)).isFail())
                .isTrue();
        assertThat(sale.addPayment(new Sale.PayCreateDTO(new BigDecimal("50.00"), PaymentMethod.EFECTIVO)).isSuccess())
                .isTrue();
        assertThat(sale.calculateAmountDue().getValue()).isEqualByComparingTo("0");
    }

    private Sale sale(String total, String paid) throws Exception {
        return new Sale(new SaleIdImpl(UUID.randomUUID()), new CustomerIdImpl(UUID.randomUUID()),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                List.of(new Sale.SaleItemRestoreDTO(new SaleDetailIdImpl(UUID.randomUUID()),
                        new ProductIdImpl(UUID.randomUUID()), new ProductQuantity(new BigDecimal("1.000")),
                        new Money(new BigDecimal(total)))),
                List.of(new Sale.PayRestoreDTO(new PayIdImpl(UUID.randomUUID()), new Money(new BigDecimal(paid)),
                        PaymentMethod.EFECTIVO, LocalDateTime.of(2026, 1, 1, 11, 0))));
    }

    private CreditNote credit(Sale sale, String unitPrice, String quantity) throws Exception {
        Sale.SaleItemReadDTO detail = sale.getSaleDetails().getFirst();
        ProductReturn productReturn = new ProductReturn(detail.saleDetailId(),
                new ProductQuantity(new BigDecimal(quantity)), ProductReturnReason.DAÑADO);
        return new CreditNote(sale.getId(), "NC-" + UUID.randomUUID(), new UserName("admin"),
                List.of(new CreditNote.Line(productReturn, new Money(new BigDecimal(unitPrice)))));
    }
}
