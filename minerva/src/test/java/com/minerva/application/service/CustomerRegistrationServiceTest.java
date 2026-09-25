package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.port.driven.AbuseAttemptRepository;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.application.port.drivers.CustomerRegistrationUseCase;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.Password;
import com.minerva.domain.valueObject.PasswordHash;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.valueObject.id.UserName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomerRegistrationServiceTest {

    @Test
    void validB2BRegistrationCreatesClientePendingAccountAndIgnoresServerOwnedFields() {
        Fixture fixture = new Fixture();

        Result<CustomerRegistrationUseCase.CustomerRegistrationResponse> result = fixture.service()
                .register(validCommand(), "203.0.113.10");

        assertTrue(result.isSuccess());
        assertEquals("cliente01", result.getData().username());
        assertEquals("PENDING_APPROVAL", result.getData().approvalStatus());
        assertEquals(1, fixture.customers.saved.size());
        assertEquals(1, fixture.users.saved.size());

        Customer customer = fixture.customers.saved.getFirst();
        User user = fixture.users.saved.getFirst();
        assertEquals("Acme Distribuciones", customer.getBusinessName().orElseThrow());
        assertEquals("20601234567", customer.getRuc().orElseThrow());
        assertEquals("cliente01", user.getUsername().getValue());
        assertEquals(com.minerva.domain.constants.Role.CLIENTE, user.getRole());
        assertEquals(AccountApprovalStatus.PENDING_APPROVAL, user.getApprovalStatus());
        assertEquals(customer.getId(), user.getCustomerId());
        assertFalse(user.canAuthenticate(), "unapproved CLIENTE must not be login-eligible");
        assertNull(user.getApprovedBy());
        assertNull(user.getRejectedBy());
    }

    @Test
    void duplicateIdentityReturnsGenericConflictAndDoesNotPersistPartialRecords() throws Exception {
        Fixture fixture = new Fixture();
        fixture.users.existingUsername = new UserName("cliente01");

        ConflictException conflict = assertThrows(ConflictException.class,
                () -> fixture.service().register(validCommand(), "203.0.113.10"));

        assertEquals("No se pudo registrar la solicitud con los datos enviados.", conflict.getMessage());
        assertTrue(fixture.customers.saved.isEmpty());
        assertTrue(fixture.users.saved.isEmpty());
        assertEquals(2, fixture.abuse.recorded.size(), "failed attempts are recorded by IP and username");
    }

    private static CustomerRegistrationUseCase.CustomerRegistrationCommand validCommand() {
        return new CustomerRegistrationUseCase.CustomerRegistrationCommand(
                "Acme Distribuciones",
                "Acme Distribuciones SAC",
                "20601234567",
                "Av. Principal 123",
                "Almacén Norte 456",
                "Ana Compras",
                "987654321",
                "12345678",
                "Ana",
                "Compras",
                "987654321",
                "ana.compras@example.com",
                "cliente01",
                "password123"
        );
    }

    private static class Fixture {
        final FakeCustomerRepository customers = new FakeCustomerRepository();
        final FakeUserRepository users = new FakeUserRepository();
        final FakeAudit audit = new FakeAudit();
        final FakeAbuseAttemptRepository abuse = new FakeAbuseAttemptRepository();
        final PasswordHasher hasher = new FakePasswordHasher();
        final CurrentUserProvider currentUserProvider = () -> new UserContext("system", "ADMIN");

        CustomerRegistrationService service() {
            return new CustomerRegistrationService(users, currentUserProvider, customers, hasher, audit,
                    new AbuseMitigationService(abuse));
        }
    }

    private static class FakePasswordHasher implements PasswordHasher {
        @Override public PasswordHash hash(Password rawPassword) {
            return assertDoesNotThrow(() ->
                    new PasswordHash("$2a$10$012345678901234567890uLxQfQO9pPGSZLxQfQO9pPGSZLxQfQO9"));
        }
        @Override public boolean matches(String password, PasswordHash hashedPassword) { return "password123".equals(password); }
    }

    private static class FakeAudit implements AuditService {
        final List<AuditEvent> events = new ArrayList<>();
        @Override public void register(AuditEvent auditEvent) { events.add(auditEvent); }
    }

    private static class FakeAbuseAttemptRepository implements AbuseAttemptRepository {
        final List<String> recorded = new ArrayList<>();
        @Override public long countRecent(String action, String subjectKey, LocalDateTime since) { return 0; }
        @Override public void record(String action, String subjectKey, boolean successful, LocalDateTime registrationDate) { recorded.add(action + ":" + successful); }
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        final List<Customer> saved = new ArrayList<>();
        boolean duplicateRuc;
        @Override public void save(Customer customer) { saved.add(customer); }
        @Override public boolean existsById(CustomerId id) { return false; }
        @Override public boolean existsByFullName(FullName fullName) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByRuc(String ruc) { return duplicateRuc; }
        @Override public Optional<Customer> findById(CustomerId id) { return saved.stream().filter(c -> c.getId().equals(id)).findFirst(); }
        @Override public Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return List.copyOf(saved); }
    }

    private static class FakeUserRepository implements UserRepository {
        final List<User> saved = new ArrayList<>();
        UserId existingUsername;
        @Override public void save(User user) { saved.add(user); }
        @Override public void save(AuditEvent auditEvent) { }
        @Override public boolean existsById(UserId id) { return existingUsername != null && existingUsername.equals(id); }
        @Override public boolean existsByDNI(DNI dni) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByEmail(Email email) { return false; }
        @Override public boolean existsByCustomerId(CustomerId customerId) { return false; }
        @Override public boolean hasUsers() { return true; }
        @Override public Optional<User> findById(UserId id) { return saved.stream().filter(u -> u.getId().equals(id)).findFirst(); }
        @Override public List<User> findClienteAccounts(AccountApprovalStatus status) { return saved.stream().filter(u -> u.getApprovalStatus() == status).toList(); }
    }
}
