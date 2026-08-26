package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierEntity {

    @Id
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "supplier_name", nullable = false, length = 100, unique = true)
    private String supplierName;

    @Column(name = "ruc", length = 11, unique = true)
    private String ruc;

    @Column(name = "phone_number", length = 9, unique = true)
    private String phoneNumber;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
}