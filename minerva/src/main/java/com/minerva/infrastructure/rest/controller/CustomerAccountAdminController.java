package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.CustomerAccountAdminUseCase;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/customer-accounts")
public class CustomerAccountAdminController {
    private final CustomerAccountAdminUseCase customerAccountAdminUseCase;

    public CustomerAccountAdminController(CustomerAccountAdminUseCase customerAccountAdminUseCase) {
        this.customerAccountAdminUseCase = customerAccountAdminUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CustomerAccountAdminUseCase.CustomerAccountSummary>> list(
            @RequestParam(defaultValue = "PENDING_APPROVAL") AccountApprovalStatus status) {
        return ResponseEntity.ok(customerAccountAdminUseCase.list(status));
    }

    @PostMapping("/{username}/approve")
    public ResponseEntity<CustomerAccountAdminUseCase.CustomerAccountSummary> approve(@PathVariable String username) {
        return ResponseEntity.ok(customerAccountAdminUseCase.approve(username));
    }

    @PostMapping("/{username}/reject")
    public ResponseEntity<CustomerAccountAdminUseCase.CustomerAccountSummary> reject(
            @PathVariable String username,
            @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(customerAccountAdminUseCase.reject(username, request.reason()));
    }

    public record RejectRequest(
            @NotBlank(message = "El motivo de rechazo es obligatorio")
            @Size(max = 255, message = "El motivo de rechazo no puede exceder 255 caracteres")
            String reason
    ) {}
}