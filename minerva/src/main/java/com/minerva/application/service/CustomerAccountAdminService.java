package com.minerva.application.service;

import com.minerva.application.exceptions.ResourceNotFoundException;
import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.UserContext;
import com.minerva.application.port.drivers.CustomerAccountAdminUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.valueObject.id.UserName;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class CustomerAccountAdminService extends Service implements CustomerAccountAdminUseCase {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerAccountAdminService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            CustomerRepository customerRepository,
            AuditService auditService
    ) {
        super(userRepository, currentUserProvider);
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAccountSummary> list(AccountApprovalStatus status) {
        requireAdmin();
        AccountApprovalStatus effectiveStatus = status == null ? AccountApprovalStatus.PENDING_APPROVAL : status;
        return userRepository.findClienteAccounts(effectiveStatus).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CustomerAccountSummary approve(String username) {
        requireAdmin();
        try {
            User user = findCliente(username);
            User approved = user.approve(new UserName(getCurrentUser().userId()));
            userRepository.save(approved);
            auditService.register(new AuditEvent(getCurrentUser().userId(), Permission.USER_REGISTER, approved));
            return toSummary(approved);
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public CustomerAccountSummary reject(String username, String reason) {
        requireAdmin();
        try {
            User user = findCliente(username);
            User rejected = user.reject(new UserName(getCurrentUser().userId()), reason);
            userRepository.save(rejected);
            auditService.register(new AuditEvent(getCurrentUser().userId(), Permission.USER_REGISTER, rejected));
            return toSummary(rejected);
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private User findCliente(String username) throws DomainException {
        User user = userRepository.findById(new UserName(username))
                .orElseThrow(() -> new ResourceNotFoundException("La solicitud de cliente no fue encontrada."));
        if (user.getRole() != Role.CLIENTE) {
            throw new ResourceNotFoundException("La solicitud de cliente no fue encontrada.");
        }
        return user;
    }

    private CustomerAccountSummary toSummary(User user) {
        Customer customer = user.getCustomerId() == null
                ? null
                : customerRepository.findById(user.getCustomerId()).orElse(null);
        String contactName = user.getPersonal().getNames().getValue() + " " + user.getPersonal().getLastNames().getValue();
        return new CustomerAccountSummary(
                user.getUsername().getValue(),
                user.getCustomerId() == null ? null : user.getCustomerId().getIdValueAsString(),
                customer == null ? null : customer.getBusinessName().orElse(null),
                customer == null ? null : customer.getLegalName().orElse(null),
                customer == null ? null : customer.getRuc().orElse(null),
                contactName,
                user.getPersonal().getPhoneNumber().getValue(),
                user.getPersonal().getEmail().getValue(),
                user.getApprovalStatus().name(),
                user.getRegistrationDate(),
                user.getApprovedDate(),
                user.getApprovedBy() == null ? null : user.getApprovedBy().getIdValueAsString(),
                user.getRejectedDate(),
                user.getRejectedBy() == null ? null : user.getRejectedBy().getIdValueAsString(),
                user.getRejectionReason()
        );
    }

    private void requireAdmin() {
        UserContext currentUser = getCurrentUser();
        if (getUserRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Solo ADMIN puede gestionar solicitudes de clientes.");
        }
    }
}