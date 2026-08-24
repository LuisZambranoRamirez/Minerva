package com.minerva.domain.entities.supplier;

import com.minerva.domain.entities.userAction.Attribute;
import com.minerva.domain.entities.userAction.DefaultDateTimeAttribute;
import com.minerva.domain.entities.userAction.DefaultStringAttribute;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.services.Result;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.RUC;
import com.minerva.domain.valueObject.id.SupplierName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Supplier extends Entity<SupplierId> {
    private final SupplierName supplierName;
    // Puede ser null
    private RUC ruc;
    private PhoneNumber phoneNumber;
    // ------------
    private final LocalDateTime registrationDate;

    public Supplier(String supplierName, String ruc, String phoneNumber) throws DomainException {
        SupplierName tempId = new SupplierName(supplierName);
        super(tempId);
        this.supplierName = tempId;
        if (ruc != null) {
            this.ruc = new RUC(ruc);
        }

        if (phoneNumber != null) {
            this.phoneNumber = new PhoneNumber(phoneNumber);
        }
        this.registrationDate = LocalDateTime.now();
    }

    public Supplier(String supplierName, String ruc, String phoneNumber, LocalDateTime registrationDate) {
        SupplierName tempId;
        try {
            tempId = new SupplierName(supplierName);
            
            this.supplierName = tempId;
            this.registrationDate = registrationDate;
            if (ruc != null) {
                this.ruc = new RUC(ruc);
            }

            if (phoneNumber != null) {
                this.phoneNumber = new PhoneNumber(phoneNumber);
            }
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el proveedor: " + e.getMessage(), e);
        }        
        super(tempId);
    }

    public Result<Void> updatePhoneNumber(String newPhoneNumber) {
        try {
            this.phoneNumber = new PhoneNumber(newPhoneNumber);
            return Result.success(null);
        } catch (InvalidDomainArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    public void removePhoneNumber() {
        this.phoneNumber = null;
    }

    public SupplierName getSupplierName() {
        return supplierName;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public Optional<RUC> getRuc() {
        return Optional.ofNullable(ruc);
    }

    public Optional<PhoneNumber> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    @Override
    public Map<String, Attribute<?>> getAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<>();

        attributes.put(
                "supplierId",
                new DefaultStringAttribute(getId().asString())
        );

        attributes.put(
                "supplierName",
                supplierName
        );

        attributes.put(
                "ruc",
                ruc
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

