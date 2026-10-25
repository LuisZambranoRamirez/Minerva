package com.minerva.infrastructure.rest.config;

import com.minerva.application.port.driven.AbuseAttemptRepository;
import com.minerva.application.port.driven.AuditService;
import com.minerva.application.port.driven.CurrentUserProvider;
import com.minerva.application.port.driven.InventoryReportRepository;
import com.minerva.application.port.driven.InventoryReportWriter;
import com.minerva.application.port.drivers.CustomerAccountAdminUseCase;
import com.minerva.application.port.drivers.CustomerRegistrationUseCase;
import com.minerva.application.port.drivers.CustomerSelfServiceUseCase;
import com.minerva.application.port.drivers.CustomerUseCase;
import com.minerva.application.port.drivers.ProductUseCase;
import com.minerva.application.port.drivers.OrderUseCase;
import com.minerva.application.port.drivers.SaleUseCase;
import com.minerva.application.port.drivers.SupplierUseCase;
import com.minerva.application.port.drivers.UserUseCase;
import com.minerva.application.port.drivers.InventoryReportUseCase;
import com.minerva.application.service.*;
import com.minerva.domain.services.PasswordHasher;
import com.minerva.domain.repositories.*;
import com.minerva.infrastructure.adapter.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

@Configuration
public class AppConfig {
    @Bean
    public CustomerUseCase customerService(UserRepository userRepository, CurrentUserProvider currentUserProvider, CustomerRepository customerRepository) {
        return new CustomerService(userRepository, currentUserProvider, customerRepository);
    }

    @Bean
    public ProductUseCase productService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                                         ProductRepository productRepository, SupplierRepository supplierRepository,
                                         InventoryMovementRepository inventoryMovementRepository) {
        return new ProductService(userRepository, currentUserProvider, productRepository, supplierRepository,
                inventoryMovementRepository);
    }

    @Bean
    public SaleUseCase saleService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                                   SaleRepository saleRepository, CustomerRepository customerRepository,
                                   ProductRepository productRepository, InventoryMovementRepository inventoryMovementRepository) {
        return new SaleService(userRepository, currentUserProvider, saleRepository, customerRepository, productRepository,
                inventoryMovementRepository);
    }

    @Bean
    public OrderUseCase orderService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                                     OrderRepository orderRepository, InventoryMovementRepository inventoryMovementRepository,
                                     ProductRepository productRepository, CustomerRepository customerRepository,
                                     SaleRepository saleRepository) {
        return new OrderService(userRepository, currentUserProvider, orderRepository, inventoryMovementRepository,
                productRepository, customerRepository, saleRepository);
    }


    @Bean
    public CustomerSelfServiceUseCase customerSelfServiceService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            InventoryMovementRepository inventoryMovementRepository) {
        return new CustomerSelfServiceService(userRepository, currentUserProvider, productRepository, orderRepository,
                customerRepository, inventoryMovementRepository);
    }

    @Bean
    public SupplierUseCase supplierService(UserRepository userRepository, CurrentUserProvider currentUserProvider, SupplierRepository supplierRepository) {
        return new SupplierService(userRepository, currentUserProvider, supplierRepository);
    }

    @Bean
    public AbuseMitigationService abuseMitigationService(AbuseAttemptRepository abuseAttemptRepository) {
        return new AbuseMitigationService(abuseAttemptRepository);
    }

    @Bean
    public UserUseCase userService(UserRepository userRepository, CurrentUserProvider currentUserProvider,
                                   PasswordHasher passwordHasher, AbuseMitigationService abuseMitigationService) {
        return new UserService(userRepository, currentUserProvider, passwordHasher, abuseMitigationService);
    }

    @Bean
    public CustomerRegistrationUseCase customerRegistrationService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            CustomerRepository customerRepository,
            PasswordHasher passwordHasher,
            AuditService auditService,
            AbuseMitigationService abuseMitigationService) {
        return new CustomerRegistrationService(userRepository, currentUserProvider, customerRepository, passwordHasher,
                auditService, abuseMitigationService);
    }

    @Bean
    public CustomerAccountAdminUseCase customerAccountAdminService(
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            CustomerRepository customerRepository,
            AuditService auditService) {
        return new CustomerAccountAdminService(userRepository, currentUserProvider, customerRepository, auditService);
    }

    @Bean
    public Clock applicationClock(@Value("${minerva.report.zone:America/Lima}") String reportZone) {
        return Clock.system(ZoneId.of(reportZone));
    }

    @Bean
    public InventoryReportUseCase inventoryReportService(
            CurrentUserProvider currentUserProvider,
            InventoryReportRepository inventoryReportRepository,
            List<InventoryReportWriter> inventoryReportWriters,
            AuditService auditService,
            Clock applicationClock) {
        return new InventoryReportService(currentUserProvider, inventoryReportRepository, inventoryReportWriters,
                auditService, applicationClock);
    }

    @Bean
    public PasswordHasher passwordHasher(){
        return new PasswordHasherAdapter();
    }
}
