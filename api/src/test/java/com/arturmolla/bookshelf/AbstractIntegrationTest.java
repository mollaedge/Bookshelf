package com.arturmolla.bookshelf;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for all Spring Boot integration tests.
 * Ensures every subclass shares the exact same Spring ApplicationContext,
 * so Testcontainers starts one container and Flyway runs exactly once.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@TestPropertySource(properties = {
        "JASYPT_ENCRYPTOR_PASSWORD=testpassword",
        "spring.profiles.active=test"
})
public abstract class AbstractIntegrationTest {
}

