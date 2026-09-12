package com.example.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests.
 *
 * Uses the Testcontainers "singleton container" pattern: containers are started
 * once in a static initializer and live for the entire JVM lifetime.  This is
 * intentional — we do NOT use @Testcontainers / @Container because the JUnit 5
 * extension tears containers down per-class (afterAll), while Spring's
 * TestContext cache keeps the ApplicationContext alive.  That mismatch caused
 * leaked contexts whose @Scheduled beans (OutboxRelayJob, etc.) retried forever
 * against destroyed containers, hanging CI for 6 hours.
 *
 * By starting containers manually with .start() and never stopping them, and
 * by registering the SAME dynamic properties in a single @DynamicPropertySource,
 * Spring's context cache key is identical across all *IT subclasses, so only
 * ONE ApplicationContext is created and reused for the whole IT suite.
 *
 * WHY real Postgres (not H2): the production schema uses PostgreSQL-specific
 * features (e.g. @SQLDelete / @SQLRestriction soft-delete, IDENTITY columns)
 * that H2 does not implement identically — an H2 integration test would give
 * false confidence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("finance_test")
                    .withUsername("test")
                    .withPassword("test");

    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        // Start once for the entire JVM — Ryuk shuts them down when the JVM exits.
        POSTGRES.start();
        RABBIT.start();
        REDIS.start();
    }

    @Autowired(required = false)
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetRedisState() {
        if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
            try (var conn = redisTemplate.getConnectionFactory().getConnection()) {
                conn.serverCommands().flushDb();
            } catch (Exception ignored) {
            }
        }
    }

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
        
        registry.add("spring.flyway.table", () -> "flyway_ci_history");

        // Disable @Scheduled jobs during integration tests so OutboxRelayJob and
        // IdempotencyKeyCleanupTask do not poll against infrastructure that may
        // be in an inconsistent state between test methods.
        registry.add("app.scheduling.enabled", () -> "false");

        // JWT secret for tests
        registry.add("jwt.secret",
                () -> "testSecretKeyForTestingOnly1234567890abcdefghijklmnop");
    }
}
