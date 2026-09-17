package com.example.repository;

import com.example.models.PasswordResetToken;
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
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("resetuser");
        user.setEmail("resetuser@example.com");
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
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("reset-token-xyz");
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        entityManager.persist(token);
        entityManager.flush();

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("reset-token-xyz");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("resetuser");
    }

    @Test
    void deleteByUser_removesToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("reset-token-to-delete");
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        entityManager.persist(token);
        entityManager.flush();

        passwordResetTokenRepository.deleteByUser(user);
        entityManager.flush();
        entityManager.clear();

        assertThat(passwordResetTokenRepository.findByToken("reset-token-to-delete")).isEmpty();
    }
}
