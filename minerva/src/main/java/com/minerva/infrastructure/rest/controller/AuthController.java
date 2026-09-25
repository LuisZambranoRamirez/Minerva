package com.minerva.infrastructure.rest.controller;


import com.minerva.application.port.drivers.CustomerRegistrationUseCase;
import com.minerva.domain.entities.user.UserReader;
import com.minerva.domain.services.Result;
import com.minerva.infrastructure.rest.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.minerva.infrastructure.rest.exception.ApiErrorResponse;

import com.minerva.application.port.drivers.UserUseCase;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserUseCase userService;
    private final CustomerRegistrationUseCase customerRegistrationUseCase;
    private final JwtService jwtService;

    public AuthController(UserUseCase userService, CustomerRegistrationUseCase customerRegistrationUseCase,
                          JwtService jwtService) {
        this.userService = userService;
        this.customerRegistrationUseCase = customerRegistrationUseCase;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {

        Result<UserReader> result = userService.authenticate(request.username(), request.password(), clientKey(servletRequest));

        if (result.isFail()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiErrorResponse.of(
                            HttpStatus.UNAUTHORIZED.value(),
                            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                            "Credenciales inválidas.",
                            "/api/v1/auth/login"));
        }

        String jwtToken = jwtService.generateToken(request.username(), result.getData().getRole());

        AuthResponse response = new AuthResponse(
                jwtToken,
                "Bearer",
                request.username(),
                result.getData().getRole().name()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/customer-register")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request,
                                              HttpServletRequest servletRequest) {
        Result<CustomerRegistrationUseCase.CustomerRegistrationResponse> result = customerRegistrationUseCase.register(
                new CustomerRegistrationUseCase.CustomerRegistrationCommand(
                        request.businessName(),
                        request.legalName(),
                        request.ruc(),
                        request.address(),
                        request.defaultDeliveryAddress(),
                        request.defaultDeliveryContact(),
                        request.defaultDeliveryPhone(),
                        request.contactDni(),
                        request.contactNames(),
                        request.contactLastNames(),
                        request.contactPhone(),
                        request.contactEmail(),
                        request.username(),
                        request.password()
                ),
                clientKey(servletRequest)
        );

        if (result.isFail()) {
            return ResponseEntity.badRequest().body(
                    ApiErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            result.getMessage(),
                            "/api/v1/auth/customer-register"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result.getData());
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record LoginRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {}

    public record CustomerRegisterRequest(
            @NotBlank(message = "La razón comercial es obligatoria") @Size(max = 150) String businessName,
            @NotBlank(message = "La razón social es obligatoria") @Size(max = 150) String legalName,
            @NotBlank(message = "El RUC es obligatorio") @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 dígitos") String ruc,
            @NotBlank(message = "La dirección fiscal/comercial es obligatoria") @Size(max = 255) String address,
            @NotBlank(message = "La dirección de entrega es obligatoria") @Size(max = 255) String defaultDeliveryAddress,
            @NotBlank(message = "El contacto de entrega es obligatorio") @Size(max = 150) String defaultDeliveryContact,
            @NotBlank(message = "El teléfono de entrega es obligatorio") @Size(max = 20) String defaultDeliveryPhone,
            @NotBlank(message = "El DNI del contacto es obligatorio") @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos") String contactDni,
            @NotBlank(message = "Los nombres del contacto son obligatorios") @Size(max = 100) String contactNames,
            @NotBlank(message = "Los apellidos del contacto son obligatorios") @Size(max = 100) String contactLastNames,
            @NotBlank(message = "El teléfono del contacto es obligatorio") @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos") String contactPhone,
            @NotBlank(message = "El correo del contacto es obligatorio") @Email @Size(max = 150) String contactEmail,
            @NotBlank(message = "El nombre de usuario es obligatorio") @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9]+$") String username,
            @NotBlank(message = "La contraseña es obligatoria") @Size(min = 8, max = 100) String password
    ) {}

    public record AuthResponse(
        String accessToken,
        String tokenType,
        String username,
        String role
    ) {}
}