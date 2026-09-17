package com.example.repository;

import com.example.models.Role;
import com.example.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword_hash("hashed_secret");
        testUser.setCreation_date(new Date());
        testUser.setRole(Role.USER);
        testUser.setPrimaryCurrency("USD");
        testUser.setActive(true);
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void findByUsername_returnsUserWhenExists() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_returnsEmptyWhenNotExists() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_returnsUserWhenExists() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void existsByUsername_returnsTrueForExisting_falseForNonExisting() {
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
    }

    @Test
    void existsByEmail_returnsTrueForExisting_falseForNonExisting() {
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    void updatePassword_updatesPasswordHashInDatabase() {
        userRepository.updatePassword(testUser.getUserId(), "new_hashed_secret");
        entityManager.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, testUser.getUserId());
        assertThat(reloaded.getPassword_hash()).isEqualTo("new_hashed_secret");
    }

    @Test
    void updateEmail_updatesEmailInDatabase() {
        userRepository.updateEmail(testUser.getUserId(), "updated@example.com");
        entityManager.flush();
        entityManager.clear();

        User reloaded = entityManager.find(User.class, testUser.getUserId());
        assertThat(reloaded.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void softDelete_respectsSqlRestriction() {
        userRepository.delete(testUser);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(testUser.getUserId());
        assertThat(found).isEmpty();
    }
}
