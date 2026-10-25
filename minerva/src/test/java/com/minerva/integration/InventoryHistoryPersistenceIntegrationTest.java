package com.minerva.integration;

import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class InventoryHistoryPersistenceIntegrationTest extends IntegrationTest {

    @Autowired InventoryMovementRepository movements;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void visibleHistoryLoadsPageWithoutPerRowProductOrActorQueries() {
        var actor = fixtures.admin();
        var product = fixtures.product(new BigDecimal("20.000"));
        for (int index = 1; index <= 5; index++) {
            jdbcTemplate.update("""
                    INSERT INTO inventory_movement (
                        inventory_movement_id, id_product, quantity, stock_before, stock_after,
                        movement_type, movement_source, source_id, actor_user_name, registration_date
                    ) VALUES (?, ?, 1, 10, 11, CAST('ENTRADA' AS inventory_movement_type),
                              CAST('STOCK_ENTRY' AS inventory_movement_source), NULL, ?, ?)
                    """, UUID.randomUUID(), product.id(), actor.username(),
                    IntegrationFixtures.FIXED_TIME.plusMinutes(index));
        }

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var page = movements.findAll(null, null, null, null, null, 0, 5);

        assertEquals(5, page.size());
        assertTrue(statistics.getPrepareStatementCount() <= 2,
                "El historial paginado no debe ejecutar consultas adicionales por producto o actor.");
        assertEquals(page.stream().map(value -> value.getRegistrationDate()).sorted().toList(),
                page.stream().map(value -> value.getRegistrationDate()).toList());
    }
}
