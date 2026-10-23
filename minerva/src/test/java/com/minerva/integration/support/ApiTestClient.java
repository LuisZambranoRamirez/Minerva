package com.minerva.integration.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiTestClient {

    private final MockMvc mockMvc;
    private final JsonTestSupport json;

    public ApiTestClient(MockMvc mockMvc, JsonTestSupport json) {
        this.mockMvc = mockMvc;
        this.json = json;
    }

    public String login(IntegrationFixtures.UserFixture user) throws Exception {
        return login(user.username(), user.password());
    }

    public String login(String username, String password) throws Exception {
        String response = loginAttempt(username, password)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.text(response, "accessToken");
    }

    public ResultActions loginAttempt(String username, String password) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json.write(Map.of("username", username, "password", password))));
    }

    public ResultActions get(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path).accept(MediaType.APPLICATION_JSON));
    }

    public ResultActions get(String path, String jwt) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON));
    }

    public ResultActions post(String path, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json.write(body)));
    }

    public ResultActions post(String path, String jwt) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON));
    }

    public ResultActions post(String path, String jwt, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json.write(body)));
    }
}
