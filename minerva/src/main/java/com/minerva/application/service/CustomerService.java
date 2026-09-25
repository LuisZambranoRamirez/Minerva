package com.minerva.application.service;

import java.util.List;
import java.util.Optional;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.drivers.CustomerUseCase;
import com.minerva.domain.constants.Permission;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.repositories.UserRepository;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.auditEvent.CollectionAuditTarget;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CustomerService extends Service implements CustomerUseCase {
    private final CustomerRepository customerRepository;

    public CustomerService(UserRepository userRepository, CurrentUserProvider currentUserProvider, CustomerRepository customerRepository) {
        super(userRepository, currentUserProvider);
        this.customerRepository = customerRepository;
    }

    // --------------------- WRITE ---------------------
    public Result<Void> registerCustomer(String fullName, String phoneNumber) {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_REGISTER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para registrar clientes.");
        }

        Customer customerCreated;
        try {
            customerCreated = new Customer(fullName, phoneNumber);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        if (customerRepository.existsByFullName(customerCreated.getFullName()))
            return Result.fail("Ya existe un cliente con el mismo nombre.");

        if (customerCreated.getPhoneNumber().isPresent() && customerRepository.existsByPhoneNumber(customerCreated.getPhoneNumber().get()))
            return Result.fail("Ya existe un cliente con el mismo número de teléfono.");

        customerRepository.save(customerCreated);
        registerUserAction(Permission.CUSTOMER_REGISTER, customerCreated.getId());
        return Result.success(null);
    }

    public Result<Void> updatePhoneNumber(String customerId, String newPhoneNumber) {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_UPDATE_PHONE_NUMBER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para actualizar el número de teléfono del cliente.");
        }

        CustomerId customerIdValue;
        try {
            customerIdValue = CustomerIdImpl.fromString(customerId);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        Optional<Customer> customerOpt = customerRepository.findById(customerIdValue);
        if (customerOpt.isEmpty()) return Result.fail("Cliente no encontrado.");

        Customer customer = customerOpt.get();

        Result<Void> updatePhoneNumberResult = customer.updatePhoneNumber(newPhoneNumber);
        if (updatePhoneNumberResult.isFail()) return updatePhoneNumberResult;

        if (customer.getPhoneNumber().isPresent() && customerRepository.existsByPhoneNumber(customer.getPhoneNumber().get()))
            return Result.fail("Ya existe un cliente con el mismo número de teléfono.");

        customerRepository.save(customer);
        registerUserAction(Permission.CUSTOMER_UPDATE_PHONE_NUMBER, customer.getId());
        return Result.success(null);
    }

    public Result<Void> removePhoneNumber(String customerId) {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_UPDATE_PHONE_NUMBER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para eliminar el número de teléfono del cliente.");
        }

        CustomerId customerIdValue;
        try {
            customerIdValue = CustomerIdImpl.fromString(customerId);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        Optional<Customer> customerOpt = customerRepository.findById(customerIdValue);
        if (customerOpt.isEmpty()) return Result.fail("Cliente no encontrado.");

        Customer customer = customerOpt.get();

        Result<Void> updatePhoneNumberResult = customer.removePhoneNumber();
        if (updatePhoneNumberResult.isFail()) return updatePhoneNumberResult;

        customerRepository.save(customer);
        registerUserAction(Permission.CUSTOMER_UPDATE_PHONE_NUMBER, customer.getId());
        return Result.success(null);
    }

    // --------------------- READ ---------------------
    public Optional<Customer> findCustomerById(String customerId) {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_FIND_BY_ID)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para buscar clientes por ID.");
        }

        try {
            return customerRepository.findById(CustomerIdImpl.fromString(customerId))
                    .map(customer -> {
                        registerUserAction(Permission.CUSTOMER_FIND_BY_ID, customer.getId());
                        return customer;
                    });
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Optional<Customer> findCustomerByPhoneNumber(String phoneNumber) {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_FIND_BY_PHONE_NUMBER)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para buscar clientes por número de teléfono.");
        }

        try {            
            return customerRepository.findByPhoneNumber(new PhoneNumber(phoneNumber))
                    .map(customer -> {
                        registerUserAction(Permission.CUSTOMER_FIND_BY_PHONE_NUMBER, customer.getId());
                        return customer;
                    });
        } catch (DomainException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public List<Customer> getAllCustomers() {
        if (getUserRole().lacksPermission(Permission.CUSTOMER_GET_ALL)) {
            throw new UnauthorizedActionException("El usuario no tiene permiso para obtener todos los clientes.");
        }

        List<Customer> customers = customerRepository.findAll();
        registerUserAction(
                Permission.CUSTOMER_GET_ALL,
                new CollectionAuditTarget(CollectionAuditTarget.Resource.CUSTOMERS)
        );
        return customers;
    }

}
