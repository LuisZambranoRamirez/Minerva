package com.minerva.domain.valueObject.id;

import com.minerva.domain.entities.supplier.SupplierId;
import com.minerva.domain.entities.userAction.StringAttribute;
import com.minerva.domain.exceptions.InvalidDomainArgumentException;
import com.minerva.domain.valueObject.ValueObject;

public class SupplierName extends ValueObject<String> implements SupplierId, StringAttribute {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 100;

    public SupplierName(String value) throws InvalidDomainArgumentException {
        super(value);
        if (value.isBlank()) throw new InvalidDomainArgumentException("El NOMBRE no puede estar vacío.");
        if (value.length() < MIN_LENGTH) throw new InvalidDomainArgumentException("El NOMBRE debe tener al menos " + MIN_LENGTH + " caracteres.");
        if (value.length() > MAX_LENGTH) throw new InvalidDomainArgumentException("El NOMBRE no puede exceder los " + MAX_LENGTH + " caracteres.");
        if (!value.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$")) throw new InvalidDomainArgumentException("El NOMBRE solo debe contener letras.");
    }

    @Override
    public String getIdValue() {
        return getValue();
    }

    @Override
    public String asString() {
        return getValue();
    }

    @Override
    public String getAttribute() {
        return getValue();
    }
}
