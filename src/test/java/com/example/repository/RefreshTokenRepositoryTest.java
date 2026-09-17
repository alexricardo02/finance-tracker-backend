package com.example.repository;

import com.example.models.RefreshToken;
import com.example.models.Role;
import com.example.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("tokenuser");
        user.setEmail("tokenuser@example.com");
        user.setPassword_hash("hash");
        user.setCreation_date(new Date());
        user.setRole(Role.USER);
        user.setPrimaryCurrency("USD");
        user.setActive(true);
        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    void findByToken_returnsMatchingToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("sample-refresh-token");
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        entityManager.persist(token);
        entityManager.flush();

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("sample-refresh-token");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("tokenuser");
    }

    @Test
    void findByUserId_returnsMatchingToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("sample-refresh-token-2");
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        entityManager.persist(token);
        entityManager.flush();

        Optional<RefreshToken> found = refreshTokenRepository.findByUserId(user.getUserId());

        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("sample-refresh-token-2");
    }

    @Test
    void deleteByUser_removesToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("to-delete");
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        entityManager.persist(token);
        entityManager.flush();

        refreshTokenRepository.deleteByUser(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("to-delete")).isEmpty();
    }

    @Test
    void deleteByToken_removesToken() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("to-delete-by-string");
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        entityManager.persist(token);
        entityManager.flush();

        refreshTokenRepository.deleteByToken("to-delete-by-string");
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("to-delete-by-string")).isEmpty();
    }
}
