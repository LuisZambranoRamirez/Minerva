package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.OrderUseCase;
import com.minerva.domain.entities.order.Order;
import com.minerva.domain.entities.order.OrderStatus;
import com.minerva.domain.entities.order.OrderTransition;
import com.minerva.infrastructure.rest.exception.BadRequestException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderUseCase orderService;

    public OrderController(OrderUseCase orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.create(new OrderUseCase.CreateOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(item -> new OrderUseCase.OrderItemCommand(
                                item.productId(), item.quantity(), item.unitPrice()))
                        .toList()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> findById(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.findById(orderId)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        OrderStatus parsedStatus = parseStatus(status);
        LocalDateTime fromDate = parseDateTime("from", from);
        LocalDateTime toDate = parseDateTime("to", to);
        return ResponseEntity.ok(orderService.findAll(parsedStatus, blankToNull(customerId), fromDate, toDate)
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/{orderId}/transitions")
    public ResponseEntity<List<OrderTransitionResponse>> findTransitions(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.findTransitions(orderId).stream()
                .map(this::mapToResponse).toList());
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirm(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.confirm(orderId)));
    }

    @PostMapping("/{orderId}/prepare")
    public ResponseEntity<OrderResponse> prepare(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.prepare(orderId)));
    }

    @PostMapping("/{orderId}/dispatch")
    public ResponseEntity<OrderResponse> dispatch(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.dispatch(orderId)));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponse> deliver(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.deliver(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable String orderId) {
        return ResponseEntity.ok(mapToResponse(orderService.cancel(orderId)));
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderDetailResponse> details = order.getDetails().stream()
                .map(detail -> new OrderDetailResponse(
                        detail.orderDetailId().getIdValueAsString(),
                        detail.productId().getIdValueAsString(),
                        detail.quantity().getValue(),
                        detail.unitPrice().getValue()
                )).toList();
        return new OrderResponse(
                order.getId().getIdValueAsString(),
                order.getCustomerId().getIdValueAsString(),
                order.getStatus(),
                order.getRegistrationDate(),
                order.getUpdatedDate(),
                order.calculateTotal().getValue(),
                details
        );
    }

    private OrderTransitionResponse mapToResponse(OrderTransition transition) {
        return new OrderTransitionResponse(
                transition.getId().getIdValueAsString(),
                transition.getPreviousStatus(),
                transition.getNewStatus(),
                transition.getActorId().getIdValueAsString(),
                transition.getRegistrationDate()
        );
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty(message = "El pedido debe incluir al menos un producto")
            List<@Valid OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            @NotBlank String productId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice
    ) {}

    public record OrderResponse(
            String orderId,
            String customerId,
            OrderStatus status,
            LocalDateTime registrationDate,
            LocalDateTime updatedDate,
            BigDecimal total,
            List<OrderDetailResponse> details
    ) {}

    public record OrderDetailResponse(
            String orderDetailId,
            String productId,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {}

    public record OrderTransitionResponse(
            String orderTransitionId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            String actorId,
            LocalDateTime registrationDate
    ) {}
}
