package com.minerva.domain.constants;

import com.minerva.domain.entities.userAction.StringAttribute;

public enum ProductCategory implements StringAttribute {
    BEBIDAS,
    ABARROTES_SECOS,
    CAFE_INFUSIONES,
    LACTEOS,
    CARNES,
    SNACKS_GOLOSINAS,
    CUIDADO_PERSONAL,
    LIMPIEZA_HOGAR,
    BEBES,
    MASCOTAS,
    OTROS;

    @Override
    public String getAttribute() {
        return name();
    }
}