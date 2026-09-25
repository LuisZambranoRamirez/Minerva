package com.minerva.domain.constants;

import java.util.Set;

public enum Role {

    ADMIN(Set.of(
            Permission.values()
    )),
    VENDEDOR(Set.of(
            Permission.CUSTOMER_REGISTER,
            Permission.CUSTOMER_UPDATE_PHONE_NUMBER,
            Permission.CUSTOMER_FIND_BY_ID,
            Permission.CUSTOMER_FIND_BY_PHONE_NUMBER,
            Permission.CUSTOMER_GET_ALL,
            Permission.PRODUCT_FIND_BY_ID,
            Permission.PRODUCT_FIND_BY_BAR_CODE,
            Permission.PRODUCT_FIND_ALL,
            Permission.PRODUCT_FIND_LOW_STOCK,
            Permission.PRODUCT_FIND_INVENTORY_MOVEMENTS,
            Permission.ORDER_CREATE,
            Permission.ORDER_CONFIRM,
            Permission.ORDER_CANCEL,
            Permission.ORDER_FIND_BY_ID,
            Permission.ORDER_FIND_ALL,
            Permission.ORDER_FIND_TRANSITIONS,
            Permission.SALE_REGISTER,
            Permission.SALE_ADD_PAYMENT,
            Permission.SALE_REGISTER_PRODUCT_RETURN,
            Permission.SALE_FIND_BY_ID,
            Permission.SALE_FIND_BY_CUSTOMER_ID,
            Permission.SALE_FIND_ALL,
            Permission.SALE_FIND_PRODUCT_RETURNS
    )),
    ALMACENISTA(Set.of(
            Permission.PRODUCT_REGISTER,
            Permission.PRODUCT_REGISTER_STOCK_ENTRY,
            Permission.PRODUCT_ASSOCIATE_UNIT_TO_BULK,
            Permission.PRODUCT_REGISTER_INVENTORY_LOSS,
            Permission.PRODUCT_FIND_BY_ID,
            Permission.PRODUCT_FIND_BY_BAR_CODE,
            Permission.PRODUCT_FIND_ALL,
            Permission.PRODUCT_FIND_INVENTORY_LOSSES,
            Permission.PRODUCT_FIND_LOW_STOCK,
            Permission.PRODUCT_FIND_INVENTORY_MOVEMENTS,
            Permission.ORDER_PREPARE,
            Permission.ORDER_DISPATCH,
            Permission.ORDER_DELIVER,
            Permission.ORDER_FIND_BY_ID,
            Permission.ORDER_FIND_ALL,
            Permission.ORDER_FIND_TRANSITIONS,
            Permission.SUPPLIER_REGISTER,
            Permission.SUPPLIER_UPDATE_PHONE_NUMBER,
            Permission.SUPPLIER_UPDATE_RUC,
            Permission.SUPPLIER_FIND_ALL,
            Permission.SUPPLIER_FIND_BY_ID,
            Permission.SUPPLIER_FIND_BY_RUC,
            Permission.SUPPLIER_FIND_BY_PHONE_NUMBER
    )),
    CLIENTE(Set.of(
            Permission.CUSTOMER_CATALOG_READ,
            Permission.CUSTOMER_ORDER_CREATE,
            Permission.CUSTOMER_ORDER_FIND_BY_ID,
            Permission.CUSTOMER_ORDER_FIND_ALL,
            Permission.CUSTOMER_ORDER_FIND_TRANSITIONS,
            Permission.CUSTOMER_ORDER_CANCEL
    ));

    private final Set<Permission> permissions;

    private Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission (Permission permission) {
        return permissions.contains(permission);
    }

    public boolean lacksPermission(Permission permission) {
        return !hasPermission(permission);
    }
}
