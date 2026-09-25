package com.minerva.application.port.drivers;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.user.UserReader;
import com.minerva.domain.services.Result;

public interface UserUseCase {
    Result<Void> register(String dni, String names, String lastNames, String phoneNumber,
                          String email, String username, String password, Role role)
            throws UnauthorizedActionException;
    Result<UserReader> authenticate(String username, String password, String clientKey);
}