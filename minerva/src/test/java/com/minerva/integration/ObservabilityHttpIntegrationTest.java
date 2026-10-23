package com.minerva.integration;

import com.minerva.integration.support.IntegrationFixtures;
import com.minerva.integration.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityHttpIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessIsPublicAndDoesNotExposeHealthDetails() throws Exception {
        api.get("/livez")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void readinessIsPublicWithDatabaseAvailableAndDoesNotExposeSensitiveDetails() throws Exception {
        api.get("/readyz")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("PostgreSQL"))));
    }

    @Test
    void prometheusRequiresAuthentication() throws Exception {
        prometheus(null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusRejectsNonAdminUsers() throws Exception {
        IntegrationFixtures.UserFixture seller = fixtures.seller();
        String jwt = api.login(seller);

        prometheus(jwt)
                .andExpect(status().isForbidden());
    }

    @Test
    void prometheusIsAvailableToAdmins() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        String jwt = api.login(admin);

        prometheus(jwt)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")));
    }

    @Test
    void unexposedActuatorEndpointReturnsNotFoundForAdmins() throws Exception {
        IntegrationFixtures.UserFixture admin = fixtures.admin();
        String jwt = api.login(admin);

        api.get("/actuator/env", jwt)
                .andExpect(status().isNotFound());
    }

    private ResultActions prometheus(String jwt) throws Exception {
        var request = get("/actuator/prometheus").accept(MediaType.TEXT_PLAIN);
        if (jwt != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        return mockMvc.perform(request);
    }
}
