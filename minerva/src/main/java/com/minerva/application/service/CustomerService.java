package com.minerva.application.service;

import java.util.List;
import java.util.Optional;

import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.exceptions.DomainException;

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // --------------------- WRITE ---------------------
    public Result<Void> registerCustomer(String customerName, String phoneNumber) {
        Customer customerCreated;
        try {
            customerCreated = new Customer(customerName, phoneNumber);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        if (customerRepository.existsById(customerCreated.getId()))
            return Result.fail("Ya existe un cliente con el mismo nombre.");

        if (customerCreated.getPhoneNumber().isPresent() && customerRepository.existsByPhoneNumber(customerCreated.getPhoneNumber().get()))
            return Result.fail("Ya existe un cliente con el mismo número de teléfono.");

        customerRepository.save(customerCreated);
        return Result.success(null);
    }

    public Result<Void> updatePhoneNumber(String customerId, String newPhoneNumber) {
        CustomerId customerIdValue;
        try {
            customerIdValue = new CustomerName(customerId);
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
        return Result.success(null);
    }

    public Result<Void> removePhoneNumber(String customerId) {
        CustomerId customerIdValue;
        try {
            customerIdValue = new CustomerName(customerId);
        } catch (DomainException e) {
            return Result.fail(e.getMessage());
        }

        Optional<Customer> customerOpt = customerRepository.findById(customerIdValue);
        if (customerOpt.isEmpty()) return Result.fail("Cliente no encontrado.");

        Customer customer = customerOpt.get();

        Result<Void> updatePhoneNumberResult = customer.removePhoneNumber();
        if (updatePhoneNumberResult.isFail()) return updatePhoneNumberResult;

        customerRepository.save(customer);
        return Result.success(null);
    }

    // --------------------- READ ---------------------
    public Optional<Customer> findCustomerById(String customerId) {
        try {
            return customerRepository.findById(new CustomerName(customerId));
        } catch (DomainException e) {
            return Optional.empty();
        }
    }

    public Optional<Customer> findCustomerByPhoneNumber(String phoneNumber) {
        try {            
            return customerRepository.findByPhoneNumber(new PhoneNumber(phoneNumber));
        } catch (DomainException e) {
            return Optional.empty();
        }
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

}
