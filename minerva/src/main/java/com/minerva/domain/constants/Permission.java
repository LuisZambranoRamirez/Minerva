package com.minerva.domain.constants;

public enum Permission {

    // Customer - Write
    CUSTOMER_REGISTER,
    CUSTOMER_UPDATE_PHONE_NUMBER,

    // Customer - Read
    CUSTOMER_FIND_BY_ID,
    CUSTOMER_FIND_BY_PHONE_NUMBER,
    CUSTOMER_GET_ALL,

    // Customer self-service - Read
    CUSTOMER_CATALOG_READ,

    // Customer self-service - Orders
    CUSTOMER_ORDER_CREATE,
    CUSTOMER_ORDER_FIND_BY_ID,
    CUSTOMER_ORDER_FIND_ALL,
    CUSTOMER_ORDER_FIND_TRANSITIONS,
    CUSTOMER_ORDER_CANCEL,
//---------------------------------------------------------
    // Product - Write
    PRODUCT_REGISTER,
    PRODUCT_REGISTER_STOCK_ENTRY,
    PRODUCT_ASSOCIATE_UNIT_TO_BULK,
    PRODUCT_REGISTER_INVENTORY_LOSS,

    // Product - Read
    PRODUCT_FIND_BY_ID,
    PRODUCT_FIND_BY_BAR_CODE,
    PRODUCT_FIND_ALL,
    PRODUCT_FIND_INVENTORY_LOSSES,
    PRODUCT_FIND_LOW_STOCK,
    PRODUCT_FIND_INVENTORY_MOVEMENTS,

//---------------------------------------------------------

    // Order - Write
    ORDER_CREATE,
    ORDER_CONFIRM,
    ORDER_PREPARE,
    ORDER_DISPATCH,
    ORDER_DELIVER,
    ORDER_CANCEL,

    // Order - Read
    ORDER_FIND_BY_ID,
    ORDER_FIND_ALL,
    ORDER_FIND_TRANSITIONS,

//---------------------------------------------------------

    // Sale - Write
    SALE_REGISTER,
    SALE_ADD_PAYMENT,
    SALE_REGISTER_PRODUCT_RETURN,

    // Sale - Read
    SALE_FIND_BY_ID,
    SALE_FIND_BY_CUSTOMER_ID,
    SALE_FIND_ALL,
    SALE_FIND_PRODUCT_RETURNS,
//---------------------------------------------------------
    // Supplier - Write
    SUPPLIER_REGISTER,
    SUPPLIER_UPDATE_PHONE_NUMBER,
    SUPPLIER_UPDATE_RUC,

    // Supplier - Read
    SUPPLIER_FIND_ALL,
    SUPPLIER_FIND_BY_ID,
    SUPPLIER_FIND_BY_RUC,
    SUPPLIER_FIND_BY_PHONE_NUMBER,
//---------------------------------------------------------

    // User - Write
    USER_REGISTER,

    // User - Auth
    USER_AUTHENTICATE,

    // User - Read
    USER_FIND_BY_USERNAME,
    USER_FIND_BY_ID,
    USER_FIND_ALL;
}
