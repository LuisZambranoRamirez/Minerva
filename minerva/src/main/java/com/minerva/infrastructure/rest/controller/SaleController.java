package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.SaleUseCase;
import com.minerva.domain.constants.PaymentMethod;
import com.minerva.domain.constants.ProductReturnReason;
import com.minerva.domain.entities.sale.ProductReturn;
import com.minerva.domain.entities.sale.Sale;
import com.minerva.domain.services.Result;
import com.minerva.infrastructure.rest.exception.BadRequestException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {
    private final SaleUseCase saleService;

    public SaleController(SaleUseCase saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<?> registerSale(@Valid @RequestBody RegisterSaleRequest request) {
        Result<Void> result = saleService.registerSale(
                request.customerId(),
                request.payments().stream().map(this::toCommand).toList(),
                request.items().stream().map(this::toCommand).toList()
        );
        if (result.isFail()) throw new BadRequestException(result.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{saleId}/payments")
    public ResponseEntity<?> addPaymentToSale(@PathVariable String saleId,
                                              @Valid @RequestBody AddPaymentRequest request) {
        Result<Void> result = saleService.addPaymentToSale(
                saleId,
                request.payments().stream().map(this::toCommand).toList()
        );
        if (result.isFail()) throw new BadRequestException(result.getMessage());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<?> findById(@PathVariable String saleId) {
        return saleService.findSaleById(saleId)
                .map(sale -> ResponseEntity.ok(mapToResponse(sale)))
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada."));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SaleResponse>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(saleService.findSalesByCustomerId(customerId).stream()
                .map(this::mapToResponse).toList());
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> findAll() {
        return ResponseEntity.ok(saleService.findAllSales().stream().map(this::mapToResponse).toList());
    }

    @PostMapping("/details/{saleDetailId}/returns")
    public ResponseEntity<?> registerProductReturn(
            @PathVariable String saleDetailId,
            @Valid @RequestBody RegisterProductReturnRequest request) {
        Result<Void> result = saleService.registerProductReturn(
                saleDetailId, request.quantity(), request.reason());
        if (result.isFail()) throw new BadRequestException(result.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/details/{saleDetailId}/returns")
    public ResponseEntity<List<ProductReturnResponse>> findProductReturnsBySaleDetail(
            @PathVariable String saleDetailId) {
        return ResponseEntity.ok(saleService.findProductReturnsBySaleDetailId(saleDetailId)
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/returns")
    public ResponseEntity<List<ProductReturnResponse>> findAllProductReturns() {
        return ResponseEntity.ok(saleService.findAllProductReturns()
                .stream().map(this::mapToResponse).toList());
    }

    private SaleUseCase.SaleItemCommand toCommand(SaleItemRequest item) {
        return new SaleUseCase.SaleItemCommand(item.productId(), item.quantity(), item.unitPrice());
    }

    private SaleUseCase.PaymentCommand toCommand(PaymentRequest payment) {
        return new SaleUseCase.PaymentCommand(payment.amount(), payment.paymentMethod());
    }

    private SaleResponse mapToResponse(Sale sale) {
        List<SaleDetailResponse> details = sale.getSaleDetails().stream()
                .map(detail -> new SaleDetailResponse(
                        detail.saleDetailId().getIdValueAsString(),
                        detail.productId().getIdValueAsString(),
                        detail.productQuantity().getValue(),
                        detail.unitPrice().getValue()
                )).toList();
        List<PaymentResponse> payments = sale.getPays().stream()
                .map(payment -> new PaymentResponse(
                        payment.payId().getIdValueAsString(),
                        payment.amount().getValue(),
                        payment.paymentMethod(),
                        payment.registrationDate()
                )).toList();

        return new SaleResponse(
                sale.getId().getIdValueAsString(),
                sale.getCustomerId().getIdValueAsString(),
                sale.getRegistrationDate(),
                sale.calculateTotal().getValue(),
                sale.calculateTotalPaid().getValue(),
                sale.calculateAmountDue().getValue(),
                details,
                payments
        );
    }

    private ProductReturnResponse mapToResponse(ProductReturn productReturn) {
        return new ProductReturnResponse(
                productReturn.getId().getIdValueAsString(),
                productReturn.getSaleDetailId().getIdValueAsString(),
                productReturn.getQuantity().getValue(),
                productReturn.getReason(),
                productReturn.getRegistrationDate()
        );
    }

    public record RegisterSaleRequest(
            @NotBlank String customerId,
            @NotEmpty(message = "La venta debe incluir al menos un producto") List<@Valid SaleItemRequest> items,
            @NotNull List<@Valid PaymentRequest> payments
    ) {}

    public record AddPaymentRequest(
            @NotEmpty(message = "Debe incluir al menos un pago") List<@Valid PaymentRequest> payments) {}

    public record RegisterProductReturnRequest(
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotNull ProductReturnReason reason
    ) {}

    public record SaleItemRequest(
            @NotBlank String productId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice
    ) {}

    public record PaymentRequest(
            @NotNull @DecimalMin(value = "0.10") BigDecimal amount,
            @NotNull PaymentMethod paymentMethod
    ) {}

    public record SaleResponse(
            String saleId,
            String customerId,
            LocalDateTime registrationDate,
            BigDecimal total,
            BigDecimal totalPaid,
            BigDecimal amountDue,
            List<SaleDetailResponse> details,
            List<PaymentResponse> payments
    ) {}

    public record SaleDetailResponse(
            String saleDetailId,
            String productId,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {}

    public record PaymentResponse(
            String payId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            LocalDateTime registrationDate
    ) {}

    public record ProductReturnResponse(
            String productReturnId,
            String saleDetailId,
            BigDecimal quantity,
            ProductReturnReason reason,
            LocalDateTime registrationDate
    ) {}
}
