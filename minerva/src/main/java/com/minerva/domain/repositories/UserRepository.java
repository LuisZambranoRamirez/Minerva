package com.minerva.domain.repositories;

import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.entities.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    void save(AuditEvent auditEvent);

    boolean existsById(UserId id);
    boolean existsByDNI(DNI dni);
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
    boolean existsByEmail(Email email);
    boolean existsByCustomerId(CustomerId customerId);
    boolean hasUsers();
    Optional<User> findById(UserId id);
    List<User> findClienteAccounts(AccountApprovalStatus status);
}