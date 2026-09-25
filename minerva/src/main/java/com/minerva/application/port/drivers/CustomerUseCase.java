package com.minerva.application.port.drivers;

import com.minerva.application.exceptions.UnauthorizedActionException;
import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.services.Result;

import java.util.List;
import java.util.Optional;

public interface CustomerUseCase {
    Result<Void> registerCustomer(String fullName, String phoneNumber) throws UnauthorizedActionException;
    Result<Void> updatePhoneNumber(String customerId, String newPhoneNumber) throws UnauthorizedActionException;
    Result<Void> removePhoneNumber(String customerId) throws UnauthorizedActionException;
    Optional<Customer> findCustomerById(String customerId) throws UnauthorizedActionException;
    Optional<Customer> findCustomerByPhoneNumber(String phoneNumber) throws UnauthorizedActionException;
    List<Customer> getAllCustomers() throws UnauthorizedActionException;
}
