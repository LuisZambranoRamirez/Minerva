package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.CustomerRegistrationUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.auditEvent.AuditEvent;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.personal.Personal;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.domain.entities.user.User;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.Password;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CustomerRegistrationService extends Service implements CustomerRegistrationUseCase {
    private static final String DUPLICATE_MESSAGE = "No se pudo registrar la solicitud con los datos enviados.";

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final AbuseMitigationService abuseMitigationService;

    public CustomerRegistrationService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            CustomerRepository customerRepository,
            PasswordHasher passwordHasher,
            AuditService auditService,
            AbuseMitigationService abuseMitigationService
    ) {
        super(userRepository, currentUserProvider);
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.abuseMitigationService = abuseMitigationService;
    }

    @Override
    public Result<CustomerRegistrationResponse> register(CustomerRegistrationCommand command, String clientKey) {
        abuseMitigationService.assertRegistrationAllowed(clientKey, command.username());

        try {
            Customer customer = createCustomer(command);
            Personal personal = new Personal(
                    command.contactDni(),
                    command.contactNames(),
                    command.contactLastNames(),
                    command.contactPhone(),
                    Role.CLIENTE,
                    command.contactEmail()
            );
            User user = new User(
                    personal,
                    command.username(),
                    passwordHasher.hash(new Password(command.password())).getValue(),
                    true,
                    personal.getRegistrationDate(),
                    customer.getId(),
                    AccountApprovalStatus.PENDING_APPROVAL,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertUnique(customer, user);

            customerRepository.save(customer);
            userRepository.save(user);
            auditService.register(new AuditEvent(user.getUsername().getValue(), Permission.CUSTOMER_REGISTER, customer));
            abuseMitigationService.recordRegistration(clientKey, command.username(), true);

            return Result.success(new CustomerRegistrationResponse(
                    user.getUsername().getValue(),
                    customer.getId().getIdValueAsString(),
                    user.getApprovalStatus().name(),
                    "Solicitud registrada. Tu cuenta queda pendiente de aprobación administrativa."
            ));
        } catch (DataIntegrityViolationException ex) {
            abuseMitigationService.recordRegistration(clientKey, command.username(), false);
            throw new ConflictException(DUPLICATE_MESSAGE);
        } catch (DomainException ex) {
            abuseMitigationService.recordRegistration(clientKey, command.username(), false);
            return Result.fail(ex.getMessage());
        } catch (RuntimeException ex) {
            abuseMitigationService.recordRegistration(clientKey, command.username(), false);
            if (isLikelyIntegrityConflict(ex)) {
                throw new ConflictException(DUPLICATE_MESSAGE);
            }
            throw ex;
        }
    }

    private Customer createCustomer(CustomerRegistrationCommand command) throws DomainException {
        String customerFullName = command.contactNames() + " " + command.contactLastNames();
        return new Customer(
                com.minerva.domain.valueObject.id.CustomerIdImpl.generate(),
                new FullName(customerFullName),
                command.contactPhone(),
                java.time.LocalDateTime.now(),
                command.businessName(),
                command.legalName(),
                command.ruc(),
                command.address(),
                command.defaultDeliveryAddress(),
                command.defaultDeliveryContact(),
                command.defaultDeliveryPhone()
        );
    }

    private void assertUnique(Customer customer, User user) {
        if (userRepository.existsById(user.getUsername())
                || userRepository.existsByDNI(user.getDni())
                || userRepository.existsByPhoneNumber(user.getPersonal().getPhoneNumber())
                || userRepository.existsByEmail(user.getPersonal().getEmail())
                || customerRepository.existsByFullName(customer.getFullName())
                || customer.getPhoneNumber().isPresent() && customerRepository.existsByPhoneNumber(customer.getPhoneNumber().get())
                || customer.getRuc().isPresent() && customerRepository.existsByRuc(customer.getRuc().get())
                || userRepository.existsByCustomerId(customer.getId())) {
            throw new ConflictException(DUPLICATE_MESSAGE);
        }
    }

    private boolean isLikelyIntegrityConflict(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException) return true;
            current = current.getCause();
        }
        return false;
    }
}