package com.minerva.domain.constants;

import com.minerva.domain.entities.userAction.StringAttribute;

public enum ProductReturnReason implements StringAttribute {
    DAÑADO, VENCIDO, ERROR_CLIENTE, OTROS;

    @Override
    public String getAttribute() {
        return name();
    }
}
