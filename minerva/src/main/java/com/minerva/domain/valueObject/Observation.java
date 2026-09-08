package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;

public class Observation extends ValueObject<String> {

    private static final int MAX_LENGTH = 250;

    public Observation(String value) throws InvalidDomainArgumentException {
        super(value);
        if (value.isBlank()) {
            throw new InvalidDomainArgumentException(
                    "La OBSERVACIÓN no puede estar vacía."
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new InvalidDomainArgumentException(
                    "La OBSERVACIÓN no puede exceder los "
                            + MAX_LENGTH + " caracteres."
            );
        }
    }
}