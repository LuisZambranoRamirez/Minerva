package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.CustomerSelfServiceUseCase;
import com.minerva.domain.entities.order.OrderStatus;
import com.minerva.infrastructure.rest.exception.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerSelfServiceController {
    private final CustomerSelfServiceUseCase customerSelfService;

    public CustomerSelfServiceController(CustomerSelfServiceUseCase customerSelfService) {
        this.customerSelfService = customerSelfService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<CustomerSelfServiceUseCase.CatalogItem>> catalog() {
        return ResponseEntity.ok(customerSelfService.listCatalog());
    }

    @GetMapping("/catalog/{productId}")
    public ResponseEntity<CustomerSelfServiceUseCase.CatalogItem> catalogItem(@PathVariable String productId) {
        return ResponseEntity.ok(customerSelfService.getCatalogItem(productId));
    }

    @PostMapping("/orders")
    public ResponseEntity<CustomerSelfServiceUseCase.OrderView> checkout(@Valid @RequestBody CheckoutRequest request) {
        CustomerSelfServiceUseCase.OrderView order = customerSelfService.checkout(new CustomerSelfServiceUseCase.CheckoutCommand(
                request.items().stream()
                        .map(item -> new CustomerSelfServiceUseCase.CheckoutItemCommand(item.productId(), item.quantity()))
                        .toList(),
                request.deliveryAddress(),
                request.deliveryContact(),
                request.deliveryPhone()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<CustomerSelfServiceUseCase.OrderView>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(customerSelfService.listOrders(parseStatus(status), parseDateTime("from", from), parseDateTime("to", to)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<CustomerSelfServiceUseCase.OrderView> order(@PathVariable String orderId) {
        return ResponseEntity.ok(customerSelfService.getOrder(orderId));
    }

    @GetMapping("/orders/{orderId}/transitions")
    public ResponseEntity<List<CustomerSelfServiceUseCase.OrderTransitionView>> transitions(@PathVariable String orderId) {
        return ResponseEntity.ok(customerSelfService.listOrderTransitions(orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<CustomerSelfServiceUseCase.OrderView> cancel(
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ResponseEntity.ok(customerSelfService.cancelOrder(orderId, request.reason()));
    }

    private OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El parámetro 'status' contiene un estado de pedido inválido.");
        }
    }

    private LocalDateTime parseDateTime(String parameterName, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("El parámetro '" + parameterName + "' debe tener formato ISO-8601, por ejemplo 2026-09-23T10:30:00.");
        }
    }

    public record CheckoutRequest(
            @NotEmpty(message = "El pedido debe incluir al menos un producto")
            List<@Valid CheckoutItemRequest> items,
            @Size(max = 255) String deliveryAddress,
            @Size(max = 150) String deliveryContact,
            @Size(max = 20) String deliveryPhone
    ) {}

    public record CheckoutItemRequest(
            @NotBlank String productId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity
    ) {}

    public record CancelOrderRequest(
            @NotBlank @Size(max = 255) String reason
    ) {}
}
