package com.minerva.domain.entities.user;

import com.minerva.domain.constants.Role;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.id.UserName;

import java.time.LocalDateTime;

public interface UserReader {

    UserId getId();

    DNI getDni();

    FullName getFullName();

    UserName getUsername();

    Role getRole();

    boolean isActive();

    LocalDateTime getRegistrationDate();

}
