package com.minerva.domain.entities.customer;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.services.Result;
import com.minerva.domain.valueObject.id.CustomerIdImpl;

import java.time.LocalDateTime;
import java.util.*;

public class Customer extends Entity<CustomerId> {
    private final FullName fullName;
    // Puede ser null
    private PhoneNumber phoneNumber;
    // ------------
    private final LocalDateTime registrationDate;

    public Customer(String fullName, String phoneNumber) throws InvalidDomainArgumentException {
        super(CustomerIdImpl.generate());
        this.fullName = new FullName(fullName);
        this.registrationDate = LocalDateTime.now();
        if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
    }

    public Customer(CustomerId customerId, FullName fullName, PhoneNumber phoneNumber, LocalDateTime registrationDate) {
        super(customerId);
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.registrationDate = registrationDate;
    }

    public FullName getFullName() {
        return fullName;
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
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
            new StringAttribute(getId()),
            new StringAttribute(fullName),
            new StringAttribute(phoneNumber),
            new StringAttribute("registrationDate", registrationDate)
        );
    }
}
