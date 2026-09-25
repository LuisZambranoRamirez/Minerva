package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "full_name", nullable = false, length = 100, unique = true)
    private String fullName;

    @Column(name = "phone_number", length = 9, unique = true)
    private String phoneNumber;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "legal_name", length = 150)
    private String legalName;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "default_delivery_address", length = 255)
    private String defaultDeliveryAddress;

    @Column(name = "default_delivery_contact", length = 150)
    private String defaultDeliveryContact;

    @Column(name = "default_delivery_phone", length = 20)
    private String defaultDeliveryPhone;
}