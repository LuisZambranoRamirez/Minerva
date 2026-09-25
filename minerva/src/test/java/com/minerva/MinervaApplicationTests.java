package com.minerva;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere PostgreSQL de integración con credenciales y migraciones disponibles")
class MinervaApplicationTests {

    @Test
    void contextLoads() {
    }
}
