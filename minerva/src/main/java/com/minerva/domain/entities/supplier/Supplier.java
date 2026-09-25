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
        this(restoreId(supplierId), restoreName(supplierName), restoreRuc(ruc), restorePhoneNumber(phoneNumber), registrationDate);
    }

    private Supplier(SupplierId supplierId, SupplierName supplierName, RUC ruc,
                     PhoneNumber phoneNumber, LocalDateTime registrationDate) {
        super(supplierId);
        this.supplierName = supplierName;
        this.ruc = ruc;
        this.phoneNumber = phoneNumber;
        this.registrationDate = registrationDate;
    }

    private static SupplierId restoreId(UUID supplierId) {
        try {
            return new SupplierIdImpl(supplierId);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al restaurar el ID del proveedor.", e);
        }
    }

    private static SupplierName restoreName(String supplierName) {
        try {
            return new SupplierName(supplierName);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al restaurar el nombre del proveedor.", e);
        }
    }

    private static RUC restoreRuc(String ruc) {
        if (ruc == null) return null;
        try {
            return new RUC(ruc);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al restaurar el RUC del proveedor.", e);
        }
    }

    private static PhoneNumber restorePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        try {
            return new PhoneNumber(phoneNumber);
        } catch (InvalidDomainArgumentException e) {
            throw new EntityRestoreException("Error al restaurar el teléfono del proveedor.", e);
        }
    }

    public Result<Void> updatePhoneNumber(String newPhoneNumber) {
        try {
            PhoneNumber newPhoneNumberValue = new PhoneNumber(newPhoneNumber);
            if (Objects.equals(phoneNumber, newPhoneNumberValue))
                return Result.fail("El nuevo número de teléfono es igual al actual.");
            this.phoneNumber = newPhoneNumberValue;
            return Result.success(null);
        } catch (InvalidDomainArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    public Result<Void> updateRuc(String newRuc) {
        try {
            RUC newRucValue = new RUC(newRuc);
            if (Objects.equals(ruc, newRucValue))
                return Result.fail("El nuevo RUC es igual al actual.");
            this.ruc = newRucValue;
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
        Set<Attribute<?>> attributes = new HashSet<>();
        attributes.add(new StringAttribute(getId()));
        attributes.add(new StringAttribute(supplierName));
        attributes.add(new StringAttribute("registrationDate", registrationDate));
        if (ruc != null) attributes.add(new StringAttribute(ruc));
        if (phoneNumber != null) attributes.add(new StringAttribute(phoneNumber));
        return attributes;
    }
}

