package com.minerva.integration.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Import({
        PostgresContainerConfig.class,
        DatabaseCleaner.class,
        IntegrationFixtures.class,
        JsonTestSupport.class,
        ApiTestClient.class,
        ConcurrentTestExecutor.class
})
public abstract class IntegrationTest {

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    @Autowired
    protected IntegrationFixtures fixtures;

    @Autowired
    protected ApiTestClient api;

    @Autowired
    protected JsonTestSupport json;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ConcurrentTestExecutor concurrent;

    @BeforeEach
    final void resetDatabase() {
        databaseCleaner.clean();
    }
}
