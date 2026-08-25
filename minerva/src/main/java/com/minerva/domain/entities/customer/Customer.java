package com.minerva.domain.entities.customer;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.services.Result;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.valueObject.id.CustomerName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class    Customer extends Entity<CustomerId> {
    private final CustomerName customerName;
    // Puede ser null
    private PhoneNumber phoneNumber;
    // ------------
    private final LocalDateTime registrationDate;

    public Customer(String customerName, String phoneNumber) throws InvalidDomainArgumentException {
        CustomerName customerNameValue = new CustomerName(customerName);
        super(customerNameValue);
        this.customerName = customerNameValue;
        if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
        this.registrationDate = LocalDateTime.now();
    }

    public Customer(String customerName, LocalDateTime registrationDate, String phoneNumber) {
        CustomerName customerNameValue;
        try {
            customerNameValue = new CustomerName(customerName);
            this.customerName = customerNameValue;
            this.registrationDate = registrationDate;
            if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al crear el cliente: " + e.getMessage(), e);
        }
        super(customerNameValue);
    }

    public CustomerName getCustomerName() {
        return customerName;
    }

    public Optional<PhoneNumber> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public Result<Void> updatePhoneNumber(String newPhoneNumber) {
        try {
            PhoneNumber newPhoneNumberValue = new PhoneNumber(newPhoneNumber);

            if (phoneNumber.equals(newPhoneNumberValue))
                return Result.fail("El nuevo número de teléfono es igual al actual.");

            phoneNumber = newPhoneNumberValue;
            return Result.success(null);
        } catch (InvalidDomainArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    public Result<Void> removePhoneNumber() {
        phoneNumber = null;
        return Result.success(null);
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public Map<String, Attribute<?>> extractAuditData() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "customerId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "customerName",
                customerName
        );

        attributes.put(
                "phoneNumber",
                phoneNumber
        );

        attributes.put(
                "registrationDate",
                new DefaultDateTimeAttribute(registrationDate)
        );

        return attributes;
    }
}
