package com.minerva.infrastructure.persistence.repository;

import com.minerva.domain.constants.Role;
import com.minerva.domain.entities.user.AccountApprovalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaAppUserRepositoryEntityGraphTest {

    @Test
    void userQueriesMappedToDomainFetchPersonalWithinTheRepositoryCall() throws NoSuchMethodException {
        assertFetchesPersonal(JpaAppUserRepository.class.getMethod("findById", String.class));
        assertFetchesPersonal(JpaAppUserRepository.class.getMethod(
                "findByPersonal_RoleOrderByRegistrationDateAsc", Role.class));
        assertFetchesPersonal(JpaAppUserRepository.class.getMethod(
                "findByPersonal_RoleAndApprovalStatusOrderByRegistrationDateAsc",
                Role.class, AccountApprovalStatus.class));
    }

    private void assertFetchesPersonal(Method method) {
        EntityGraph graph = method.getAnnotation(EntityGraph.class);

        assertNotNull(graph, method.getName() + " must define an entity graph");
        assertTrue(Arrays.asList(graph.attributePaths()).contains("personal"),
                method.getName() + " must fetch personal before returning AppUserEntity");
    }
}
