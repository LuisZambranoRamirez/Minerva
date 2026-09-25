package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;

public final class Email extends ValueObject<String> {

    private static final int MAX_LENGTH = 150;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email(String value) throws InvalidDomainArgumentException {
        super(value);

        if (value.isBlank()) {
            throw new InvalidDomainArgumentException("El correo electrónico no puede estar vacío.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidDomainArgumentException("El correo electrónico no puede exceder los 150 caracteres.");
        }
        if (!value.matches(EMAIL_PATTERN)) {
            throw new InvalidDomainArgumentException("El correo electrónico no tiene un formato válido.");
        }
    }
}
