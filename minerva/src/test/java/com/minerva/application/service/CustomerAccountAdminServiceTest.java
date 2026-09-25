package com.minerva.application.service;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.entities.user.UserId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.DNI;
import com.minerva.domain.valueObject.Email;
import com.minerva.domain.valueObject.FullName;
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

class CustomerAccountAdminServiceTest {

    @Test
    void adminApprovesPendingClienteAndStoresAuditMetadata() throws Exception {
        Fixture fixture = Fixture.admin();
        User pending = cliente("cliente01", AccountApprovalStatus.PENDING_APPROVAL);
        fixture.users.put(pending);
        fixture.customers.put(b2bCustomer(pending.getCustomerId()));

        var summary = fixture.service().approve("cliente01");

        User approved = fixture.users.findById(new UserName("cliente01")).orElseThrow();
        assertEquals("APPROVED", summary.approvalStatus());
        assertEquals(AccountApprovalStatus.APPROVED, approved.getApprovalStatus());
        assertEquals("admin01", approved.getApprovedBy().getIdValueAsString());
        assertNotNull(approved.getApprovedDate());
        assertNull(approved.getRejectedBy());
        assertEquals(1, fixture.audit.events.size());
    }

    @Test
    void adminRejectsPendingClienteWithRequiredReasonAndLocksFurtherApproval() throws Exception {
        Fixture fixture = Fixture.admin();
        fixture.users.put(cliente("cliente01", AccountApprovalStatus.PENDING_APPROVAL));

        var summary = fixture.service().reject("cliente01", "RUC no validado");

        User rejected = fixture.users.findById(new UserName("cliente01")).orElseThrow();
        assertEquals("REJECTED", summary.approvalStatus());
        assertEquals(AccountApprovalStatus.REJECTED, rejected.getApprovalStatus());
        assertEquals("admin01", rejected.getRejectedBy().getIdValueAsString());
        assertEquals("RUC no validado", rejected.getRejectionReason());
        assertThrows(IllegalArgumentException.class, () -> fixture.service().approve("cliente01"));
    }

    @Test
    void nonAdminCannotApproveOrRejectCustomerAccounts() throws Exception {
        Fixture fixture = Fixture.vendedor();
        fixture.users.put(cliente("cliente01", AccountApprovalStatus.PENDING_APPROVAL));

        assertThrows(UnauthorizedActionException.class, () -> fixture.service().approve("cliente01"));
        assertThrows(UnauthorizedActionException.class, () -> fixture.service().reject("cliente01", "No aplica"));
    }

    private static Customer b2bCustomer(CustomerId customerId) throws DomainException {
        return new Customer(customerId, new FullName("Cliente Empresa"), "987654321", LocalDateTime.now(),
                "Acme", "Acme SAC", "20601234567", "Av. 1", "Av. 2", "Ana", "987654321");
    }

    private static User cliente(String username, AccountApprovalStatus status) throws DomainException {
        return new User(
                new Personal("12345678", "Cliente", "Uno", "987654321", Role.CLIENTE, username + "@example.com"),
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
                status == AccountApprovalStatus.REJECTED ? "Rechazado" : null
        );
    }

    private static class Fixture {
        final FakeUserRepository users = new FakeUserRepository();
        final FakeCustomerRepository customers = new FakeCustomerRepository();
        final FakeAudit audit = new FakeAudit();
        final CurrentUserProvider currentUserProvider;

        private Fixture(Role role) { this.currentUserProvider = () -> new UserContext("admin01", role.name()); }
        static Fixture admin() { return new Fixture(Role.ADMIN); }
        static Fixture vendedor() { return new Fixture(Role.VENDEDOR); }
        CustomerAccountAdminService service() { return new CustomerAccountAdminService(users, currentUserProvider, customers, audit); }
    }

    private static class FakeAudit implements AuditService {
        final List<AuditEvent> events = new ArrayList<>();
        @Override public void register(AuditEvent auditEvent) { events.add(auditEvent); }
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        final Map<CustomerId, Customer> customers = new HashMap<>();
        void put(Customer customer) { customers.put(customer.getId(), customer); }
        @Override public void save(Customer customer) { put(customer); }
        @Override public boolean existsById(CustomerId id) { return customers.containsKey(id); }
        @Override public boolean existsByFullName(FullName fullName) { return false; }
        @Override public boolean existsByPhoneNumber(PhoneNumber phoneNumber) { return false; }
        @Override public boolean existsByRuc(String ruc) { return false; }
        @Override public Optional<Customer> findById(CustomerId id) { return Optional.ofNullable(customers.get(id)); }
        @Override public Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber) { return Optional.empty(); }
        @Override public List<Customer> findAll() { return List.copyOf(customers.values()); }
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
