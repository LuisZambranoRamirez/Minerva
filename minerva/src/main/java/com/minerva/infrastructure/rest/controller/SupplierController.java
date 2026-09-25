package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.SupplierUseCase;
import com.minerva.domain.services.Result;
import com.minerva.domain.entities.supplier.Supplier;
import com.minerva.infrastructure.rest.exception.BadRequestException;
import com.minerva.application.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierUseCase supplierService;

    public SupplierController(SupplierUseCase supplierService) {
        this.supplierService = supplierService;
    }

    // --------------------- WRITE ---------------------

    @PostMapping
    public ResponseEntity<?> registerSupplier(
            @Valid @RequestBody RegisterSupplierRequest request) {

        Result<Void> result = supplierService.register(
                request.supplierName(),
                request.ruc(),
                request.phoneNumber()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{supplierId}/phone-number")
    public ResponseEntity<?> updatePhoneNumber(
            @PathVariable String supplierId,
            @Valid @RequestBody UpdatePhoneRequest request) {

        Result<Void> result = supplierService.updatePhoneNumber(
                supplierId,
                request.phoneNumber()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{supplierId}/ruc")
    public ResponseEntity<?> updateRuc(
            @PathVariable String supplierId,
            @Valid @RequestBody UpdateRucRequest request) {

        Result<Void> result = supplierService.updateRuc(
                supplierId,
                request.ruc()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    // --------------------- READ ---------------------

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAll() {

        List<SupplierResponse> suppliers = supplierService.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<?> findById(@PathVariable String supplierId) {

        return supplierService.findById(supplierId)
                .map(supplier -> ResponseEntity.ok(mapToResponse(supplier)))
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<?> findByRuc(@PathVariable String ruc) {

        return supplierService.findByRuc(ruc)
                .map(supplier -> ResponseEntity.ok(mapToResponse(supplier)))
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<?> findByPhone(@PathVariable String phoneNumber) {

        return supplierService.findByPhone(phoneNumber)
                .map(supplier -> ResponseEntity.ok(mapToResponse(supplier)))
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));
    }

    // --------------------- MAPPER ---------------------

    private SupplierResponse mapToResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId().getIdValueAsString(),
                supplier.getSupplierName().getValue(),
                supplier.getRuc().map(r -> r.getValue()).orElse(null),
                supplier.getPhoneNumber().map(p -> p.getValue()).orElse(null)
        );
    }

    // --------------------- DTOs ---------------------

    public record RegisterSupplierRequest(
            @NotBlank String supplierName,
            String ruc,
            String phoneNumber
    ) {}

    public record UpdatePhoneRequest(
            @NotBlank String phoneNumber
    ) {}

    public record UpdateRucRequest(
            @NotBlank String ruc
    ) {}

    public record SupplierResponse(
            String supplierId,
            String supplierName,
            String ruc,
            String phoneNumber
    ) {}
}
