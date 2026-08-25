package com.example.integration;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests.
 *
 * WHY containers are static: Testcontainers reuses the same container
 * instance across all subclass test methods (JVM-lifecycle), which avoids
 * the expensive stop/start penalty for each @Test.  All containers share
 * a single Docker network created by Testcontainers.
 *
 * WHY real Postgres (not H2): the production schema uses PostgreSQL-specific
 * features (e.g. @SQLDelete / @SQLRestriction soft-delete, IDENTITY columns)
 * that H2 does not implement identically — an H2 integration test would give
 * false confidence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("finance_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @Container
    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Postgres
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.data.redis.ssl.enabled", () -> "false");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", () -> String.valueOf(RABBIT.getAmqpPort()));
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.rabbitmq.addresses", () ->
                "amqp://" + RABBIT.getAdminUsername() + ":" + RABBIT.getAdminPassword()
                        + "@" + RABBIT.getHost() + ":" + RABBIT.getAmqpPort());

        // Disable SSL for test containers
        registry.add("spring.rabbitmq.ssl.enabled", () -> "false");

        // Stub-out external mail so no SMTP connection is attempted
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "2525");
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        registry.add("management.health.mail.enabled", () -> "false");

        // JWT secret for tests
        registry.add("jwt.secret",
                () -> "testSecretKeyForTestingOnly1234567890abcdefghijklmnop");
    }
}
