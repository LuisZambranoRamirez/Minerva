package com.minerva.infrastructure.bootstrap;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.services.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final boolean enabled;
    private final String dni;
    private final String names;
    private final String lastNames;
    private final String phone;
    private final String email;
    private final String username;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            @Value("${minerva.bootstrap.enabled:false}") boolean enabled,
            @Value("${minerva.bootstrap.admin.dni:}") String dni,
            @Value("${minerva.bootstrap.admin.names:}") String names,
            @Value("${minerva.bootstrap.admin.last-names:}") String lastNames,
            @Value("${minerva.bootstrap.admin.phone:}") String phone,
            @Value("${minerva.bootstrap.admin.email:}") String email,
            @Value("${minerva.bootstrap.admin.username:}") String username,
            @Value("${minerva.bootstrap.admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.enabled = enabled;
        this.dni = dni;
        this.names = names;
        this.lastNames = lastNames;
        this.phone = phone;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (userRepository.hasUsers()) {
            log.info("Bootstrap de administrador omitido: ya existen usuarios.");
            return;
        }

        validateRequiredConfiguration();

        try {
            Personal personal = new Personal(
                    dni,
                    names,
                    lastNames,
                    phone,
                    Role.ADMIN,
                    email
            );
            User admin = new User(passwordHasher, personal, username, password);

            userRepository.save(admin);
            log.info("Administrador inicial creado correctamente con username '{}'.", username);
        } catch (DomainException e) {
            throw new IllegalStateException(
                    "No se pudo crear el administrador inicial: " + e.getMessage(),
                    e
            );
        }
    }

    private void validateRequiredConfiguration() {
        requireValue(dni, "MINERVA_ADMIN_DNI");
        requireValue(names, "MINERVA_ADMIN_NAMES");
        requireValue(lastNames, "MINERVA_ADMIN_LAST_NAMES");
        requireValue(phone, "MINERVA_ADMIN_PHONE");
        requireValue(email, "MINERVA_ADMIN_EMAIL");
        requireValue(username, "MINERVA_ADMIN_USERNAME");
        requireValue(password, "MINERVA_ADMIN_PASSWORD");
    }

    private void requireValue(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno requerida para el bootstrap: " + variableName
            );
        }
    }
}
