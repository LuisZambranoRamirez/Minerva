package com.minerva.domain.repositories;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.entities.sale.CreditNote;
import com.minerva.domain.entities.sale.SaleDetailId;
import com.minerva.domain.entities.sale.SaleId;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.entities.order.OrderId;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.valueObject.ProductQuantity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

public interface SaleRepository {
    void save(Sale sale, Set<Product> products);
    Optional<Sale> findBySourceOrderId(OrderId orderId);
    Optional<Sale> findById(SaleId id);
    default Optional<Sale> findByIdForUpdate(SaleId id) { return findById(id); }
    List<Sale> findByCustomerId(CustomerId customerId);
    List<Sale> findAll();
    void updatePayments(Sale sale);
    Optional<ReturnContext> findReturnContextForUpdate(SaleDetailId saleDetailId);
    boolean existsSaleDetailById(SaleDetailId saleDetailId);
    void saveProductReturn(ProductReturn productReturn, Product product, UserId actor);
    List<ProductReturn> findProductReturnsBySaleDetailId(SaleDetailId saleDetailId);
    List<ProductReturn> findAllProductReturns();
    default void saveCreditNote(CreditNote creditNote, Map<ProductId, Product> changedProducts) {
        throw new UnsupportedOperationException("El repositorio no implementa notas de crédito.");
    }
    default List<CreditNote> findCreditNotesBySaleId(SaleId saleId) { return List.of(); }

    record ReturnContext(ProductId productId, ProductQuantity soldQuantity,
                         ProductQuantity returnedQuantity) {}
}
