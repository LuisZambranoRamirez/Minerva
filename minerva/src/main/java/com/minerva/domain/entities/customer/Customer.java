package com.minerva.domain.entities.customer;

import com.minerva.domain.entities.Entity;
import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.EntityRestoreException;
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
    private final String businessName;
    private final String legalName;
    private final String ruc;
    private final String address;
    private final String defaultDeliveryAddress;
    private final String defaultDeliveryContact;
    private final String defaultDeliveryPhone;

    public Customer(String fullName, String phoneNumber) throws InvalidDomainArgumentException {
        super(CustomerIdImpl.generate());
        this.fullName = new FullName(fullName);
        this.registrationDate = LocalDateTime.now();
        this.businessName = null;
        this.legalName = null;
        this.ruc = null;
        this.address = null;
        this.defaultDeliveryAddress = null;
        this.defaultDeliveryContact = null;
        this.defaultDeliveryPhone = null;
        if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
    }

    public Customer(CustomerId customerId, FullName fullName, String phoneNumber, LocalDateTime registrationDate) {
        this(customerId, fullName, phoneNumber, registrationDate, null, null, null, null, null, null, null);
    }

    public Customer(
            CustomerId customerId,
            FullName fullName,
            String phoneNumber,
            LocalDateTime registrationDate,
            String businessName,
            String legalName,
            String ruc,
            String address,
            String defaultDeliveryAddress,
            String defaultDeliveryContact,
            String defaultDeliveryPhone
    ) {
        super(customerId);
        this.fullName = fullName;
        this.registrationDate = registrationDate;
        this.businessName = normalizeNullable(businessName);
        this.legalName = normalizeNullable(legalName);
        this.ruc = validateNullableRuc(ruc);
        this.address = normalizeNullable(address);
        this.defaultDeliveryAddress = normalizeNullable(defaultDeliveryAddress);
        this.defaultDeliveryContact = normalizeNullable(defaultDeliveryContact);
        this.defaultDeliveryPhone = normalizeNullable(defaultDeliveryPhone);
        try {
            if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al restaurar el cliente.", e);
        }
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

            if (Objects.equals(phoneNumber, newPhoneNumberValue))
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
    public Optional<String> getBusinessName() {
        return Optional.ofNullable(businessName);
    }

    public Optional<String> getLegalName() {
        return Optional.ofNullable(legalName);
    }

    public Optional<String> getRuc() {
        return Optional.ofNullable(ruc);
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<String> getDefaultDeliveryAddress() {
        return Optional.ofNullable(defaultDeliveryAddress);
    }

    public Optional<String> getDefaultDeliveryContact() {
        return Optional.ofNullable(defaultDeliveryContact);
    }

    public Optional<String> getDefaultDeliveryPhone() {
        return Optional.ofNullable(defaultDeliveryPhone);
    }

    private static String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String validateNullableRuc(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) return null;
        if (!normalized.matches("\\d{11}")) {
            throw new EntityRestoreException("El RUC del cliente debe tener 11 digitos.");
        }
        return normalized;
    }


    @Override
    public Set<Attribute<?>> getAuditData() {
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute(getId()));
        attributes.add(new StringAttribute(fullName));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        if (phoneNumber != null) attributes.add(new StringAttribute(phoneNumber));
        if (businessName != null) attributes.add(new StringAttribute("businessName", businessName));
        if (legalName != null) attributes.add(new StringAttribute("legalName", legalName));
        if (ruc != null) attributes.add(new StringAttribute("ruc", ruc));
        return attributes;
    }
}
