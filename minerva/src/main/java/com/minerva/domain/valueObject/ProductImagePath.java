package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;

public final class ProductImagePath extends ValueObject<String> {
    public static final int MAX_LENGTH = 120;
    private static final String PATTERN = "^products/[a-z0-9]+(?:-[a-z0-9]+)*\\.webp$";

    public ProductImagePath(String value) throws InvalidDomainArgumentException {
        super(value);
        if (value.length() > MAX_LENGTH || !value.matches(PATTERN)) {
            throw new InvalidDomainArgumentException("La imagen debe ser una ruta WebP segura con formato products/<archivo>.webp.");
        }
    }
}
