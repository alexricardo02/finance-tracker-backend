package com.example.repository;

import com.example.models.IdempotencyKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class IdempotencyKeyRepositoryTest {

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByIdempotencyKey_returnsMatchingKey() {
        IdempotencyKey key = new IdempotencyKey();
        key.setIdempotencyKey("uuid-12345");
        key.setResponseStatus(200);
        key.setResponseBody("{\"success\":true}");
        key.setUserId(1);

        entityManager.persist(key);
        entityManager.flush();

        Optional<IdempotencyKey> found = idempotencyKeyRepository.findByIdempotencyKey("uuid-12345");

        assertThat(found).isPresent();
        assertThat(found.get().getResponseStatus()).isEqualTo(200);
        assertThat(found.get().getResponseBody()).isEqualTo("{\"success\":true}");
    }

    @Test
    void deleteByCreatedAtBefore_deletesOldKeys_retainsNewKeys() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);

        IdempotencyKey oldKey = new IdempotencyKey();
        oldKey.setIdempotencyKey("old-key");
        oldKey.setResponseStatus(200);
        oldKey.setResponseBody("old");
        oldKey.setUserId(1);
        oldKey.setCreatedAt(cutoff.minus(2, ChronoUnit.HOURS));

        IdempotencyKey newKey = new IdempotencyKey();
        newKey.setIdempotencyKey("new-key");
        newKey.setResponseStatus(200);
        newKey.setResponseBody("new");
        newKey.setUserId(1);
        newKey.setCreatedAt(cutoff.plus(2, ChronoUnit.HOURS));

        entityManager.persist(oldKey);
        entityManager.persist(newKey);
        entityManager.flush();

        idempotencyKeyRepository.deleteByCreatedAtBefore(cutoff);
        entityManager.flush();
        entityManager.clear();

        assertThat(idempotencyKeyRepository.findByIdempotencyKey("old-key")).isEmpty();
        assertThat(idempotencyKeyRepository.findByIdempotencyKey("new-key")).isPresent();
    }
}
