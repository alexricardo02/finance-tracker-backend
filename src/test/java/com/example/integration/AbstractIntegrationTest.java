package com.example.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redis.testcontainers.RedisContainer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

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
// Spring Boot disables metrics export in tests by default, which would hide /actuator/prometheus.
@AutoConfigureObservability(tracing = false)
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

    // One static server shared by all ITs: a per-class server would change the bean
    // overrides below and break the single cached ApplicationContext.
    protected static final WireMockServer FX_API =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        FX_API.start();
    }

    @TestBean(name = "dolarApiClient", methodName = "stubbedFxClient")
    private RestClient dolarApiClient;

    @TestBean(name = "frankfurterClient", methodName = "stubbedFxClient")
    private RestClient frankfurterClient;

    @MockitoBean
    protected JavaMailSender mailSender;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    // The production clients have no read timeout, so the stub sets one to keep a
    // delayed response from hanging the suite.
    static RestClient stubbedFxClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(1));
        return RestClient.builder().baseUrl(FX_API.baseUrl()).requestFactory(requestFactory).build();
    }

    protected static void stubDolarOficial(double venta) {
        FX_API.stubFor(get(urlPathEqualTo("/v1/dolares/oficial")).willReturn(okJson(
                "{\"compra\":" + (venta - 10) + ",\"venta\":" + venta
                        + ",\"casa\":\"oficial\",\"fechaActualizacion\":\"2026-07-01T00:00:00Z\"}")));
    }

    protected static void stubFrankfurter(String from, String to, String date, double rate) {
        FX_API.stubFor(get(urlPathEqualTo("/v1/" + date))
                .withQueryParam("base", com.github.tomakehurst.wiremock.client.WireMock.equalTo(from))
                .withQueryParam("symbols", com.github.tomakehurst.wiremock.client.WireMock.equalTo(to))
                .willReturn(okJson("{\"rates\":{\"" + to + "\":" + rate + "}}")));
    }

    // The breaker is a singleton in the shared context, so failures in one test would
    // otherwise short-circuit calls in the next.
    @BeforeEach
    void resetFxStubs() {
        FX_API.resetAll();
        circuitBreakerRegistry.circuitBreaker("exchangeRate").reset();
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
