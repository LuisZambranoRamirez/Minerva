package com.minerva.application.port.drivers;

import com.minerva.domain.services.Result;

public interface CustomerRegistrationUseCase {
    Result<CustomerRegistrationResponse> register(CustomerRegistrationCommand command, String clientKey);

    record CustomerRegistrationCommand(
            String businessName,
            String legalName,
            String ruc,
            String address,
            String defaultDeliveryAddress,
            String defaultDeliveryContact,
            String defaultDeliveryPhone,
            String contactDni,
            String contactNames,
            String contactLastNames,
            String contactPhone,
            String contactEmail,
            String username,
            String password
    ) {}

    record CustomerRegistrationResponse(
            String username,
            String customerId,
            String approvalStatus,
            String message
    ) {}
}