package com.minerva.application.service;

import java.util.Optional;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.UserUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.user.UserReader;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.services.Result;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.id.UserName;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class UserService extends Service implements UserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AbuseMitigationService abuseMitigationService;

    public UserService(UserRepository userRepository, CurrentUserProvider currentUserProvider, PasswordHasher passwordHasher,
                       AbuseMitigationService abuseMitigationService) {
        super(userRepository, currentUserProvider);
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.abuseMitigationService = abuseMitigationService;
    }

    // --------------------- WRITE ---------------------
    public Result<Void> register(
            String dni,
            String names,
            String lastNames,
            String phoneNumber,
            String email,
            String username,
            String password,
            Role role
    ) {
        if (getUserRole().lacksPermission(Permission.USER_REGISTER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para registrar usuarios.");
        }

        User userCreated;
        try {
            Personal personal = new Personal(
                    dni,
                    names,
                    lastNames,
                    phoneNumber,
                    role,
                    email
            );
            userCreated = new User(passwordHasher, personal, username, password);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        if (userRepository.existsById(userCreated.getUsername()))
            return Result.fail("Ya existe el usuario");

        if (userRepository.existsByDNI(userCreated.getDni()))
            return Result.fail("El DNI esta registrado con otro usuario");

        if (userRepository.existsByPhoneNumber(userCreated.getPersonal().getPhoneNumber()))
            return Result.fail("El número de teléfono está registrado con otra persona");

        if (userRepository.existsByEmail(userCreated.getPersonal().getEmail()))
            return Result.fail("El correo electrónico está registrado con otra persona");

        userRepository.save(userCreated);
        registerUserAction(Permission.USER_REGISTER, userCreated.getId());
        return Result.success(null);
    }

    public Result<UserReader> authenticate(String username, String password, String clientKey) {
        abuseMitigationService.assertLoginAllowed(clientKey, username);
        UserName userName;
        try {
            userName = new UserName(username);
        } catch (DomainException e) {
            abuseMitigationService.recordLogin(clientKey, username, false);
            return Result.fail("Credenciales invalidas");
        }

        Optional<User> userOptional = userRepository.findById(userName);

        if (userOptional.isEmpty()) {
            abuseMitigationService.recordLogin(clientKey, username, false);
            return Result.fail("Credenciales invalidas");
        }

        User user = userOptional.get();

        if (user.authenticate(password, passwordHasher)) {
            abuseMitigationService.recordLogin(clientKey, username, true);
            return Result.success(user);
        } else {
            abuseMitigationService.recordLogin(clientKey, username, false);
            return Result.fail("Credenciales invalidas");
        }
    }

}
