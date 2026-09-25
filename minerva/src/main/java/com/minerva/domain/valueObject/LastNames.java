package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;

public final class LastNames extends ValueObject<String> {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    public LastNames(String value) throws InvalidDomainArgumentException {
        super(value);

        if (value.isBlank()) {
            throw new InvalidDomainArgumentException("Los apellidos no pueden estar vacíos.");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidDomainArgumentException("Los apellidos deben tener entre 2 y 100 caracteres.");
        }
        if (!value.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]+$")) {
            throw new InvalidDomainArgumentException("Los apellidos solo pueden contener letras y espacios.");
        }
    }
}
