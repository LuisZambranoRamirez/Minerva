package com.minerva.domain.entities.supplier;

import com.minerva.domain.entities.auditEvent.Attribute;
import com.minerva.domain.entities.auditEvent.StringAttribute;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.services.Result;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.entities.Entity;
import com.minerva.domain.valueObject.RUC;
import com.minerva.domain.valueObject.SupplierName;
import com.minerva.domain.valueObject.id.SupplierIdImpl;

import java.time.LocalDateTime;
import java.util.*;

public class Supplier extends Entity<SupplierId> {
    private final SupplierName supplierName;
    // Puede ser null
    private RUC ruc;
    private PhoneNumber phoneNumber;
    // ------------
    private final LocalDateTime registrationDate;

    public Supplier(String supplierName, String ruc, String phoneNumber) throws DomainException {
        super(SupplierIdImpl.generate());
        if (ruc != null) this.ruc = new RUC(ruc);
        if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
        this.supplierName = new SupplierName(supplierName);
        this.registrationDate = LocalDateTime.now();
    }

    public Supplier(UUID supplierId, String supplierName, String ruc, String phoneNumber, LocalDateTime registrationDate) {
        SupplierId supplierIdValue;
        try {
            supplierIdValue = new SupplierIdImpl(supplierId);
            this.supplierName = new SupplierName(supplierName);
            this.registrationDate = registrationDate;
            if (ruc != null) this.ruc = new RUC(ruc);
            if (phoneNumber != null) this.phoneNumber = new PhoneNumber(phoneNumber);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al crear el proveedor: " + e.getMessage(), e);
        }        
        super(supplierIdValue);
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
    public Set<Attribute<?>> getAuditData() {
        return Set.of(
                new StringAttribute(getId()),
                new StringAttribute(supplierName),
                new StringAttribute(ruc),
                new StringAttribute(phoneNumber),
                new StringAttribute("registration_date", registrationDate)
        );
    }
}

