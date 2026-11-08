package com.example.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyMigrationIT extends AbstractIntegrationTest {

    private static final String INSERT = "INSERT INTO idempotency_keys "
            + "(idempotency_key, user_id, response_status, response_body, created_at) VALUES (?, ?, 201, '{}', now())";

    private DriverManagerDataSource dataSource(String jdbcUrl) {
        return new DriverManagerDataSource(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    // A separate database, because the shared one has already passed V3 and V1 hardcodes the public schema.
    private DriverManagerDataSource freshDatabase() {
        String name = "migration_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(dataSource(POSTGRES.getJdbcUrl())).execute("CREATE DATABASE " + name);
        return dataSource(POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + name));
    }

    private Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure().dataSource(dataSource).table("flyway_ci_history").target(target).load();
    }

    @Test
    void v3_keepsExistingRowsWithNoUserAndMakesKeysUniquePerUser() {
        DriverManagerDataSource dataSource = freshDatabase();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        flyway(dataSource, "2").migrate();
        jdbc.update(INSERT, "legacy-a", 0);
        jdbc.update(INSERT, "legacy-b", 0);

        flyway(dataSource, "3").migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_keys WHERE user_id = 0", Integer.class)).isEqualTo(2);
        jdbc.update(INSERT, "legacy-a", 5);
        jdbc.update(INSERT, "legacy-a", 6);
        assertThatThrownBy(() -> jdbc.update(INSERT, "legacy-a", 5)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update(INSERT, "legacy-a", 0)).isInstanceOf(DuplicateKeyException.class);
    }
}
