package com.minerva.infrastructure.rest.controller;

import com.minerva.application.port.drivers.UserUseCase;
import com.minerva.domain.constants.Role;
import com.minerva.domain.services.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.minerva.infrastructure.rest.exception.BadRequestException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserUseCase userService;

    public UserController(UserUseCase userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request) {
        Result<Void> result = userService.register(
                request.dni(),
                request.names(),
                request.lastNames(),
                request.phoneNumber(),
                request.email(),
                request.username(),
                request.password(),
                request.role()
        );

        if (result.isFail()) {
            throw new BadRequestException(result.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record RegisterUserRequest(
            @NotBlank String dni,
            @NotBlank String names,
            @NotBlank String lastNames,
            @NotBlank String phoneNumber,
            @NotBlank @Email String email,
            @NotBlank String username,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
            @NotNull Role role
    ) {
    }
}
