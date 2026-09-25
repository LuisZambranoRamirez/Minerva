package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.customer.Customer;
import com.minerva.domain.entities.customer.CustomerId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.valueObject.FullName;
import com.minerva.domain.valueObject.PhoneNumber;
import com.minerva.domain.repositories.CustomerRepository;
import com.minerva.domain.valueObject.id.CustomerIdImpl;
import com.minerva.infrastructure.persistence.entity.CustomerEntity;
import com.minerva.infrastructure.persistence.repository.JpaCustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final JpaCustomerRepository jpaCustomerRepository;

    public CustomerRepositoryAdapter(JpaCustomerRepository jpaCustomerRepository) {
        this.jpaCustomerRepository = jpaCustomerRepository;
    }

    @Override
    public void save(Customer customer) {
        jpaCustomerRepository.saveAndFlush(toEntity(customer));
    }

    @Override
    public boolean existsById(CustomerId id) {
        return jpaCustomerRepository.existsById(id.getIdValue());
    }

    @Override
    public boolean existsByFullName(FullName fullName) {
        return jpaCustomerRepository.existsByFullName(fullName.getValue());
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        return jpaCustomerRepository.existsByPhoneNumber(phoneNumber.getValue());
    }

    @Override
    public boolean existsByRuc(String ruc) {
        return ruc != null && jpaCustomerRepository.existsByRuc(ruc);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaCustomerRepository.findById(id.getIdValue())
                .map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber) {
        return jpaCustomerRepository.findByPhoneNumber(phoneNumber.getValue())
                .map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaCustomerRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Customer toDomain(CustomerEntity entity) {
        try {
            return new Customer(
                    new CustomerIdImpl(entity.getCustomerId()),
                    new FullName(entity.getFullName()),
                    entity.getPhoneNumber(),
                    entity.getRegistrationDate(),
                    entity.getBusinessName(),
                    entity.getLegalName(),
                    entity.getRuc(),
                    entity.getAddress(),
                    entity.getDefaultDeliveryAddress(),
                    entity.getDefaultDeliveryContact(),
                    entity.getDefaultDeliveryPhone()
            );
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el cliente.", e);
        }
    }

    private CustomerEntity toEntity(Customer customer) {
        return CustomerEntity.builder()
                .customerId(customer.getId().getIdValue())
                .fullName(customer.getFullName().getValue())
                .phoneNumber(customer.getPhoneNumber().map(PhoneNumber::getValue).orElse(null))
                .registrationDate(customer.getRegistrationDate())
                .businessName(customer.getBusinessName().orElse(null))
                .legalName(customer.getLegalName().orElse(null))
                .ruc(customer.getRuc().orElse(null))
                .address(customer.getAddress().orElse(null))
                .defaultDeliveryAddress(customer.getDefaultDeliveryAddress().orElse(null))
                .defaultDeliveryContact(customer.getDefaultDeliveryContact().orElse(null))
                .defaultDeliveryPhone(customer.getDefaultDeliveryPhone().orElse(null))
                .build();
    }

}
