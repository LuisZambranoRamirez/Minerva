package com.minerva.domain.valueObject;

import com.minerva.domain.exceptions.InvalidDomainArgumentException;

public class SKU extends ValueObject<String> {

    // Patrón: 1 letra mayúscula + 3 alfanuméricos (mayúsculas), un guion,
    // 4 alfanuméricos (mayúsculas), un guion, 4 alfanuméricos (mayúsculas).
    // Ejemplo válido: ABCD-1234-EFGH o A1B2-C3D4-E5F6
    private static final String SKU_PATTERN = "^[A-Z][A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{4}$";

    public SKU(String value) throws InvalidDomainArgumentException {
        super(value);

        if (!value.matches(SKU_PATTERN)) {
            throw new InvalidDomainArgumentException(
                    "El SKU debe tener el formato correcto: 3 grupos de 4 caracteres (letras MAYÚSCULAS y números) " +
                            "separados por guiones medios y empezar obligatoriamente con una letra mayúscula (ej. ABCD-1234-EFGH)."
            );
        }
    }
}