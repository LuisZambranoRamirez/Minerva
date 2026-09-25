package com.minerva.application.port.drivers;

import com.minerva.domain.services.Result;
import com.minerva.domain.entities.supplier.Supplier;
import com.minerva.application.exceptions.UnauthorizedActionException;
import java.util.List;
import java.util.Optional;

public interface SupplierUseCase {

    // --------------------- WRITE ---------------------
    Result<Void> register(String supplierName, String ruc, String phoneNumber) throws UnauthorizedActionException;
    Result<Void> updatePhoneNumber(String supplierId, String phoneNumber) throws UnauthorizedActionException;
    Result<Void> updateRuc(String supplierId, String ruc) throws UnauthorizedActionException;

    // --------------------- READ ---------------------
    List<Supplier> findAll() throws UnauthorizedActionException;
    Optional<Supplier> findById(String supplierId)throws UnauthorizedActionException;
    Optional<Supplier> findByRuc(String ruc)throws UnauthorizedActionException;
    Optional<Supplier> findByPhone(String phone)throws UnauthorizedActionException;
}
