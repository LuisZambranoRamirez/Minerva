package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.CustomerUseCase;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.services.Result;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerUseCase customerService;

    public CustomerController(CustomerUseCase customerService) {
        this.customerService = customerService;
    }

    // --------------------- WRITE ---------------------
    @PostMapping
    public ResponseEntity<?> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {

        Result<Void> result = customerService.registerCustomer(
                request.fullName(),
                request.phoneNumber()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{customerId}/phone-number")
    public ResponseEntity<?> updatePhoneNumber(
            @PathVariable String customerId,
            @Valid @RequestBody UpdatePhoneRequest request) {

        Result<Void> result = customerService.updatePhoneNumber(
                customerId,
                request.newPhoneNumber()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    // --------------------- READ ---------------------

    @GetMapping("/{customerId}")
    public ResponseEntity<?> findById(@PathVariable String customerId) {
        return customerService.findCustomerById(customerId)
                .map(customer -> ResponseEntity.ok(toResponse(customer)))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {

        List<CustomerResponse> customers =
                customerService.getAllCustomers()
                        .stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/search")
    public ResponseEntity<?> findByPhoneNumber(@RequestParam String phoneNumber) {
        return customerService.findCustomerByPhoneNumber(phoneNumber)
                .map(customer -> ResponseEntity.ok(toResponse(customer)))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId().getIdValueAsString(),
                customer.getFullName().getValue(),
                customer.getPhoneNumber()
                        .map(phone -> phone.getValue())
                        .orElse(null),
                customer.getRegistrationDate().toString()
        );
    }

    // --------------------- DTOs ---------------------

    public record RegisterCustomerRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String fullName,

        String phoneNumber

    ) {}

    public record UpdatePhoneRequest(
        @NotBlank(message = "El número de teléfono es obligatorio")
        String newPhoneNumber
    ) {}

    public record CustomerResponse(
        String customerId,
        String fullName,
        String phoneNumber,
        String registrationDate
    ) {
    }
}
