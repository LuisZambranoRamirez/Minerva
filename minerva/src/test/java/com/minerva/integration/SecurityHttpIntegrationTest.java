package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHttpIntegrationTest extends IntegrationTest {

    private static final String ADMIN_ENDPOINT = "/api/v1/admin/customer-accounts";

    @Test
    void validLoginReturnsBearerJwt() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();

        api.loginAttempt(admin.username(), admin.password())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(admin.username()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();

        api.loginAttempt(admin.username(), "wrong-password")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        api.get(ADMIN_ENDPOINT)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithWrongRoleReturnsForbidden() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        String jwt = api.login(seller);

        api.get(ADMIN_ENDPOINT, jwt)
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointWithInvalidTokenReturnsUnauthorized() throws Exception {
        api.get(ADMIN_ENDPOINT, "not-a-valid-jwt")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithAllowedRoleSucceedsUsingRealLogin() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        String jwt = api.login(admin);

        api.get(ADMIN_ENDPOINT, jwt)
                .andExpect(status().isOk());
    }
}
