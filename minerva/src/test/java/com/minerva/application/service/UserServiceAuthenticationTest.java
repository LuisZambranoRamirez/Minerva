package com.minerva.application.service;

import com.minerva.application.port.driven.AbuseAttemptRepository;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.Password;
import com.minerva.domain.valueObject.PasswordHash;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceAuthenticationTest {

    @Test
    void pendingAndRejectedClienteCannotAuthenticateEvenWithValidPassword() throws Exception {
        Fixture fixture = new Fixture();
        fixture.users.put(cliente("pending01", AccountApprovalStatus.PENDING_APPROVAL));
        fixture.users.put(cliente("rejected01", AccountApprovalStatus.REJECTED));

        Result<?> pending = fixture.service().authenticate("pending01", "password123", "203.0.113.10");
        Result<?> rejected = fixture.service().authenticate("rejected01", "password123", "203.0.113.10");

        assertTrue(pending.isFail());
        assertTrue(rejected.isFail());
        assertEquals("Credenciales invalidas", pending.getMessage());
        assertEquals("Credenciales invalidas", rejected.getMessage());
        assertEquals(4, fixture.abuse.recorded.size(), "denials are recorded by IP and username without enumeration");
    }

    @Test
    void approvedClienteCanAuthenticateAndExposesClienteRoleForJwtClaims() throws Exception {
        Fixture fixture = new Fixture();
        fixture.users.put(cliente("approved01", AccountApprovalStatus.APPROVED));

        Result<?> result = fixture.service().authenticate("approved01", "password123", "203.0.113.10");

        assertTrue(result.isSuccess());
        assertEquals(Role.CLIENTE, ((com.minerva.domain.entities.user.UserReader) result.getData()).getRole());
        assertTrue(fixture.abuse.recorded.stream().allMatch(entry -> entry.endsWith(":true")));
    }

    private static User cliente(String username, AccountApprovalStatus status) throws DomainException {
        return new User(
                new Personal("12345678", "Cliente", "Perez", "987654321", Role.CLIENTE, username + "@example.com"),
                username,
                "hash",
                true,
                LocalDateTime.now(),
                CustomerIdImpl.generate(),
                status,
                null,
                null,
                null,
                null,
                status == AccountApprovalStatus.REJECTED ? "Datos inválidos" : null
        );
    }

    private static class Fixture {
        final FakeUserRepository users = new FakeUserRepository();
        final FakeAbuseAttemptRepository abuse = new FakeAbuseAttemptRepository();
        final CurrentUserProvider currentUserProvider = () -> new UserContext("system", "ADMIN");
        final FakePasswordHasher passwordHasher;

        Fixture() throws DomainException {
            passwordHasher = new FakePasswordHasher();
        }

        UserService service() { return new UserService(users, currentUserProvider, passwordHasher, new AbuseMitigationService(abuse)); }
    }

    private static class FakePasswordHasher implements PasswordHasher {
        private final PasswordHash hash;

        FakePasswordHasher() throws DomainException {
            hash = new PasswordHash("hash");
        }

        @Override public PasswordHash hash(Password rawPassword) { return hash; }
        @Override public boolean matches(String password, PasswordHash hashedPassword) { return "password123".equals(password); }
    }

    private static class FakeAbuseAttemptRepository implements AbuseAttemptRepository {
        final List<String> recorded = new ArrayList<>();
        @Override public long countRecent(String action, String subjectKey, LocalDateTime since) { return 0; }
        @Override public void record(String action, String subjectKey, boolean successful, LocalDateTime registrationDate) { recorded.add(action + ":" + successful); }
    }

    private static class FakeUserRepository implements UserRepository {
        final Map<UserId, User> users = new HashMap<>();
        void put(User user) { users.put(user.getId(), user); }
        @Override public void save(User user) { put(user); }
        @Override public void save(AuditEvent auditEvent) { }
        @Override public boolean existsById(UserId id) { return users.containsKey(id); }
        @Override public boolean existsByDNI(DNI dni) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByEmail(Email email) { return false; }
        @Override public boolean existsByCustomerId(CustomerId customerId) { return false; }
        @Override public boolean hasUsers() { return true; }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(users.get(id)); }
        @Override public List<User> findClienteAccounts(AccountApprovalStatus status) { return users.values().stream().filter(u -> u.getApprovalStatus() == status).toList(); }
    }
}
