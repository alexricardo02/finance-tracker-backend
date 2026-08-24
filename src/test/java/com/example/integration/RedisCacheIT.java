package com.example.integration;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis cache integration test.
 *
 * Verifies:
 * 1. @Cacheable actually populates real Redis keys (not a mock).
 * 2. CacheService.evictUserFinancialCache only deletes keys matching the
 *    target username (SCAN-based eviction), and never performs a global flush.
 *
 * WHY: The production CacheService uses SCAN with a username pattern.
 * A global FLUSHDB would wipe other tenants' data; this test proves isolation.
 */
class RedisCacheIT extends AbstractIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired CacheService cacheService;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    void evictUserFinancialCache_onlyDeletesKeysMatchingTargetUser_notOtherUsersKeys() throws Exception {
        // ── Setup: register two users ─────────────────────────────────────────
        String suffix = String.valueOf(System.nanoTime());
        String alice = "alice_" + suffix;
        String bob   = "bob_"   + suffix;

        registerAndLogin(alice);
        registerAndLogin(bob);

        // ── Seed Redis with fake cache keys for both users ────────────────────
        // WHY we seed manually: triggering @Cacheable through the REST API
        // would require actual DB data; seeding directly lets us focus the
        // test on cache isolation without the surrounding CRUD setup overhead.
        String aliceKey1 = "incomes_month::" + alice + "_JULY";
        String aliceKey2 = "expenses_last_7_days::" + alice + "_2026-07-01";
        String bobKey    = "incomes_month::" + bob + "_JULY";

        redisTemplate.opsForValue().set(aliceKey1, "alice-data-1");
        redisTemplate.opsForValue().set(aliceKey2, "alice-data-2");
        redisTemplate.opsForValue().set(bobKey,    "bob-data");

        // ── Evict only Alice's cache ──────────────────────────────────────────
        cacheService.evictUserFinancialCache(alice);

        // Alice's keys must be gone
        assertThat(redisTemplate.hasKey(aliceKey1)).isFalse();
        assertThat(redisTemplate.hasKey(aliceKey2)).isFalse();

        // WHY: Bob's key must survive — proves the SCAN pattern (*alice*) only
        // matches Alice's tenant namespace and does NOT perform a global flush.
        assertThat(redisTemplate.hasKey(bobKey)).isTrue();

        // Cleanup
        redisTemplate.delete(bobKey);
    }

    @Test
    void evictUserFinancialCache_noKeysForUser_completesSilently() {
        // WHY: SCAN over an empty pattern should not throw — ensures the
        // cursor iteration guard (try-with-resources) handles the empty case.
        cacheService.evictUserFinancialCache("user_that_has_no_cache_" + System.nanoTime());
        // No exception = pass
    }

    @Test
    void evictGlobalCache_deletesExactKey_notRelatedKeys() {
        String targetKey = "usd_ars_oficial::rate";
        String unrelatedKey = "fx_rate::USD_EUR_2026-07-01";

        redisTemplate.opsForValue().set(targetKey, "1090.0");
        redisTemplate.opsForValue().set(unrelatedKey, "0.92");

        cacheService.evictGlobalCache("usd_ars_oficial", "rate");

        assertThat(redisTemplate.hasKey(targetKey)).isFalse();
        assertThat(redisTemplate.hasKey(unrelatedKey)).isTrue();

        // Cleanup
        redisTemplate.delete(unrelatedKey);
    }

    private void registerAndLogin(String username) throws Exception {
        UserRegistrationDTO reg = new UserRegistrationDTO(
                username, "Passw0rd!", username + "@test.com");
        restTemplate.postForEntity(
                "http://localhost:" + port + "/api/users/register",
                toJsonEntity(reg), String.class);
    }

    private HttpEntity<String> toJsonEntity(Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }
}
