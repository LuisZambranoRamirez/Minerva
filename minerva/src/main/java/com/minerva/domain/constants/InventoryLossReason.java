package com.minerva.domain.constants;

import com.minerva.domain.entities.userAction.StringAttribute;

public enum InventoryLossReason implements StringAttribute {
    DAÑADO, VENCIMIENTO, PERDIDO, ROBO, OTROS;

    @Override
    public String getAttribute() {
        return name();
    }
}
