package com.minerva.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaProductRepositoryLowStockQueryTest {

    @Test
    void lowStockQueryRequiresThresholdAndIncludesBoundary() throws NoSuchMethodException {
        Method method = JpaProductRepository.class.getMethod("findLowStockProducts");
        Query query = method.getAnnotation(Query.class);

        String jpql = query.value().toLowerCase();

        assertTrue(jpql.contains("reorderlevel is not null"), "products without reorderLevel must be excluded");
        assertTrue(jpql.contains("stock <=") || jpql.contains("<= p.reorderlevel"), "stock equal to reorderLevel is low stock");
    }
}
