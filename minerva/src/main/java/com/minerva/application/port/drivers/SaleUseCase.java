package com.minerva.application.port.drivers;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.services.Result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SaleUseCase {
    record SaleItemCommand(String productId, BigDecimal quantity, BigDecimal unitPrice) {}
    record PaymentCommand(BigDecimal amount, PaymentMethod paymentMethod) {}

    Result<Void> registerSale(String customerId, List<PaymentCommand> payments,
                              List<SaleItemCommand> items) throws UnauthorizedActionException;
    Result<Void> addPaymentToSale(String saleId, List<PaymentCommand> payments)
            throws UnauthorizedActionException;
    Optional<Sale> findSaleById(String saleId) throws UnauthorizedActionException;
    List<Sale> findSalesByCustomerId(String customerId) throws UnauthorizedActionException;
    List<Sale> findAllSales() throws UnauthorizedActionException;
    Result<Void> registerProductReturn(String saleDetailId, BigDecimal quantity,
                                       ProductReturnReason reason) throws UnauthorizedActionException;
    List<ProductReturn> findProductReturnsBySaleDetailId(String saleDetailId)
            throws UnauthorizedActionException;
    List<ProductReturn> findAllProductReturns() throws UnauthorizedActionException;
}
