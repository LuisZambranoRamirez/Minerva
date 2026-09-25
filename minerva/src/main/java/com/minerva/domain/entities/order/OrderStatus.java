package com.minerva.domain.entities.order;

public enum OrderStatus {
    PENDIENTE,
    CONFIRMADO,
    EN_PREPARACION,
    EN_REPARTO,
    ENTREGADO,
    CANCELADO;

    public boolean isTerminal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    public boolean canBeCancelled() {
        return this == PENDIENTE || this == CONFIRMADO || this == EN_PREPARACION;
    }

    public boolean requiresStockRestorationOnCancel() {
        return this == CONFIRMADO || this == EN_PREPARACION;
    }
}