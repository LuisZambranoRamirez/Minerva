package com.minerva.infrastructure.rest.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import com.minerva.domain.constants.ProductCategory;
import com.minerva.domain.constants.InventoryLossReason;
import com.minerva.domain.entities.product.InventoryLoss;
import com.minerva.domain.entities.inventory.InventoryMovement;
import com.minerva.domain.entities.inventory.InventoryMovementSource;
import com.minerva.domain.entities.inventory.InventoryMovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minerva.application.port.drivers.ProductUseCase;
import com.minerva.domain.constants.GainStrategy;
import com.minerva.domain.constants.SaleType;
import com.minerva.domain.entities.product.Product;
import com.minerva.domain.services.Result;
import com.minerva.infrastructure.rest.exception.BadRequestException;
import com.minerva.application.exceptions.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductUseCase productService;

    public ProductController(ProductUseCase productService) {
        this.productService = productService;
    }

    // --------------------- WRITE ---------------------

    @PostMapping
    public ResponseEntity<?> registerProduct(@Valid @RequestBody RegisterProductRequest request) {

        Result<Void> result = productService.registerProduct(
                request.sku(),
                request.productName(),
                request.gainStrategy(),
                request.gainAmount(),
                request.reorderLevel(),
                request.barCode(),
                request.saleType(),
                request.productCategory(),
                request.purchasedFromSupplierId(),
                request.purchaseUnitPrice(),
                request.purchaseQuantity(),
                request.purchaseExpirationDate()
        );

        if (result.isFail()) throw new BadRequestException(result.getMessage());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{productId}/stock-entries")
    public ResponseEntity<?> registerStockEntry(
            @PathVariable String productId,
            @Valid @RequestBody RegisterStockEntryRequest request) {

        Result<Void> result = productService.registerStockEntry(
                productId,
                request.supplierId(),
                request.unitPrice(),
                request.quantity(),
                request.expirationDate()
        );

        if (result.isFail()) throw new BadRequestException(result.getMessage());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/unit-to-bulk")
    public ResponseEntity<?> registerUnitToBulk(
            @Valid @RequestBody RegisterUnitToBulkRequest request) {

        Result<Void> result = productService.registerUnitToBulk(
                request.unitProductId(),
                request.bulkProductId(),
                request.quantity()
        );

        if (result.isFail()) throw new BadRequestException(result.getMessage());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{productId}/inventory-losses")
    public ResponseEntity<?> registerInventoryLoss(
            @PathVariable String productId,
            @Valid @RequestBody RegisterInventoryLossRequest request) {
        Result<Void> result = productService.registerInventoryLoss(
                productId,
                request.quantity(),
                request.reason(),
                request.observation()
        );

        if (result.isFail()) throw new BadRequestException(result.getMessage());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --------------------- READ ---------------------

    @GetMapping("/{productId}")
    public ResponseEntity<?> findById(@PathVariable String productId) {

        return productService.findProductById(productId)
                .map(product -> ResponseEntity.ok(mapToResponse(product)))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
    }

    @GetMapping("/barcode/{barCode}")
    public ResponseEntity<?> findByBarCode(@PathVariable String barCode) {

        return productService.findProductByBarCode(barCode)
                .map(product -> ResponseEntity.ok(mapToResponse(product)))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> products = productService.findAllProducts()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> findLowStockProducts() {
        return ResponseEntity.ok(productService.findLowStockProducts()
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> findInventoryMovementsByProductId(
            @PathVariable String productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(productService.findInventoryMovements(
                        productId, parseMovementType(type), parseMovementSource(source),
                        parseDateTime("from", from), parseDateTime("to", to), page, size)
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/inventory-movements")
    public ResponseEntity<List<InventoryMovementResponse>> findInventoryMovements(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(productService.findInventoryMovements(
                        productId, parseMovementType(type), parseMovementSource(source),
                        parseDateTime("from", from), parseDateTime("to", to), page, size)
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/{productId}/inventory-losses")
    public ResponseEntity<List<InventoryLossResponse>> findInventoryLossesByProductId(
            @PathVariable String productId) {
        return ResponseEntity.ok(productService.findInventoryLossesByProductId(productId)
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/inventory-losses")
    public ResponseEntity<List<InventoryLossResponse>> findAllInventoryLosses() {
        return ResponseEntity.ok(productService.findAllInventoryLosses()
                .stream().map(this::mapToResponse).toList());
    }

    @GetMapping("/inventory-losses/reason/{reason}")
    public ResponseEntity<List<InventoryLossResponse>> findInventoryLossesByReason(
            @PathVariable InventoryLossReason reason) {
        return ResponseEntity.ok(productService.findInventoryLossesByReason(reason)
                .stream().map(this::mapToResponse).toList());
    }

    // --------------------- MAPPER ---------------------

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId().getIdValueAsString(),
                product.getSku().getValue(),
                product.getProductName().getValue(),
                product.getBarCode().map(b -> b.getValue()).orElse(null),
                product.getSaleType().name(),
                product.getCategory().name(),
                product.getStock().getValue(),
                product.getReorderLevel().map(value -> value.getValue()).orElse(null)
        );
    }

    private InventoryMovementResponse mapToResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId().getIdValueAsString(),
                movement.getProductId().getIdValueAsString(),
                movement.getQuantity(),
                movement.getStockBefore().getValue(),
                movement.getStockAfter().getValue(),
                movement.getType(),
                movement.getSource(),
                movement.getSourceId() == null ? null : movement.getSourceId().toString(),
                movement.getActorId().getIdValueAsString(),
                movement.getRegistrationDate()
        );
    }

    private InventoryLossResponse mapToResponse(InventoryLoss inventoryLoss) {
        return new InventoryLossResponse(
                inventoryLoss.getId().getIdValueAsString(),
                inventoryLoss.getProductId().getIdValueAsString(),
                inventoryLoss.getQuantity().getValue(),
                inventoryLoss.getReason(),
                inventoryLoss.getObservation().map(value -> value.getValue()).orElse(null),
                inventoryLoss.getRegistrationDate()
        );
    }


    private InventoryMovementType parseMovementType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return InventoryMovementType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El parámetro 'type' contiene un tipo de movimiento inválido.");
        }
    }

    private InventoryMovementSource parseMovementSource(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return InventoryMovementSource.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("El parámetro 'source' contiene un origen de movimiento inválido.");
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

    // --------------------- DTOs ---------------------

    public record RegisterProductRequest(
            @NotBlank String sku,
            @NotBlank String productName,
            @NotNull GainStrategy gainStrategy,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal gainAmount,
            @DecimalMin(value = "0.0") BigDecimal reorderLevel,
            String barCode,
            @NotNull SaleType saleType,
            @NotNull ProductCategory productCategory,
            @NotBlank String purchasedFromSupplierId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal purchaseUnitPrice,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal purchaseQuantity,
            @NotNull LocalDateTime purchaseExpirationDate
    ) {}

    public record RegisterStockEntryRequest(
            @NotBlank String supplierId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotNull LocalDateTime expirationDate
    ) {}

    public record RegisterUnitToBulkRequest(
            @NotBlank String unitProductId,
            @NotBlank String bulkProductId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity
    ) {}

    public record RegisterInventoryLossRequest(
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotNull InventoryLossReason reason,
            String observation
    ) {}

    public record ProductResponse(
            String productId,
            String sku,
            String productName,
            String barCode,
            String saleType,
            String category,
            BigDecimal stock,
            BigDecimal reorderLevel
    ) {}

    public record InventoryMovementResponse(
            String inventoryMovementId,
            String productId,
            BigDecimal quantity,
            BigDecimal stockBefore,
            BigDecimal stockAfter,
            InventoryMovementType type,
            InventoryMovementSource source,
            String sourceId,
            String actorId,
            LocalDateTime registrationDate
    ) {}

    public record InventoryLossResponse(
            String inventoryLossId,
            String productId,
            BigDecimal quantity,
            InventoryLossReason reason,
            String observation,
            LocalDateTime registrationDate
    ) {}
}
