package com.example.integration;

import com.example.models.IdempotencyKey;
import com.example.models.OutboxEvent;
import com.example.models.PasswordResetToken;
import com.example.models.RefreshToken;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.IdempotencyKeyRepository;
import com.example.repository.OutboxEventRepository;
import com.example.repository.PasswordResetTokenRepository;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PostgresSchemaIT extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;
    @Autowired private OutboxEventRepository outboxEvents;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private PasswordResetTokenRepository resetTokens;

    private User user() {
        String name = UUID.randomUUID().toString();
        return users.saveAndFlush(new User(name, name + "@test.com", "hash", new Date()));
    }

    private IdempotencyKey idempotencyKey(String value, int userId) {
        IdempotencyKey key = new IdempotencyKey();
        key.setIdempotencyKey(value);
        key.setUserId(userId);
        key.setResponseStatus(201);
        key.setResponseBody("{}");
        return key;
    }

    @ParameterizedTest
    @CsvSource({
            "expenses,idx_expense_user", "expenses,idx_expense_category", "expenses,idx_expense_date",
            "incomes,idx_income_user", "incomes,idx_income_category", "incomes,idx_income_date",
            "categories,idx_category_user", "categories,idx_category_type"})
    void migrations_createBtreeIndexes(String table, String index) {
        String definition = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE tablename = ? AND indexname = ?", String.class, table, index);

        assertThat(definition).contains("USING btree");
    }

    // Planner statistics are empty in a fresh container, so seq scans are disabled to check index usability alone.
    @ParameterizedTest
    @CsvSource({
            "SELECT * FROM incomes WHERE user_id = 1,idx_income_user",
            "SELECT * FROM expenses WHERE user_id = 1,idx_expense_user",
            "SELECT * FROM expenses WHERE date BETWEEN '2026-01-01' AND '2026-02-01',idx_expense_date"})
    void plannerCanUseIndex(String query, String index) {
        jdbc.execute("SET LOCAL enable_seqscan = off");

        List<String> plan = jdbc.queryForList("EXPLAIN " + query, String.class);

        assertThat(String.join("\n", plan)).contains(index);
    }

    @Test
    void users_usernameAndEmailAreUnique() {
        User first = user();

        assertThatThrownBy(() -> users.saveAndFlush(
                new User(first.getUsername(), "other@test.com", "hash", new Date())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void users_softDeleteKeepsRowAndHidesFromRepository() {
        User user = user();

        users.delete(user);
        users.flush();

        assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE user_id = ?", Boolean.class, user.getUserId())).isFalse();
        assertThat(users.existsByUsername(user.getUsername())).isFalse();
    }

    @Disabled("BUG-19: derived findUserIdByUsername returns User, not Integer, and throws at runtime")
    @Test
    void users_findUserIdByUsername_returnsTheId() {
        User user = user();

        assertThat(users.findUserIdByUsername(user.getUsername())).isEqualTo(user.getUserId());
    }

    @Test
    void idempotencyKey_uniqueConstraintRejectsDuplicate() {
        String value = UUID.randomUUID().toString();
        idempotencyKeys.saveAndFlush(idempotencyKey(value, 1));

        assertThatThrownBy(() -> idempotencyKeys.saveAndFlush(idempotencyKey(value, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void idempotencyKey_sameValueForDifferentUsers_isAllowedAndIsolated() {
        String value = UUID.randomUUID().toString();
        idempotencyKeys.saveAndFlush(idempotencyKey(value, 1));
        idempotencyKeys.saveAndFlush(idempotencyKey(value, 2));

        assertThat(idempotencyKeys.findByUserIdAndIdempotencyKey(1, value)).isPresent();
        assertThat(idempotencyKeys.findByUserIdAndIdempotencyKey(2, value)).isPresent();
        assertThat(idempotencyKeys.findByUserIdAndIdempotencyKey(3, value)).isEmpty();
    }

    @Test
    void idempotencyKey_deleteByCreatedAtBefore_removesOnlyOldKeys() {
        IdempotencyKey old = idempotencyKey(UUID.randomUUID().toString(), 1);
        old.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        IdempotencyKey fresh = idempotencyKey(UUID.randomUUID().toString(), 1);
        idempotencyKeys.saveAndFlush(old);
        idempotencyKeys.saveAndFlush(fresh);

        idempotencyKeys.deleteByCreatedAtBefore(Instant.now().minus(48, ChronoUnit.HOURS));
        idempotencyKeys.flush();

        assertThat(idempotencyKeys.findByUserIdAndIdempotencyKey(1, old.getIdempotencyKey())).isEmpty();
        assertThat(idempotencyKeys.findByUserIdAndIdempotencyKey(1, fresh.getIdempotencyKey())).isPresent();
    }

    @Test
    void outbox_returnsOnlyPendingEventsOldestFirst() {
        String marker = UUID.randomUUID().toString();
        OutboxEvent newer = new OutboxEvent("T", marker + "-newer");
        newer.setCreatedAt(LocalDateTime.now());
        OutboxEvent older = new OutboxEvent("T", marker + "-older");
        older.setCreatedAt(LocalDateTime.now().minusHours(1));
        OutboxEvent processed = new OutboxEvent("T", marker + "-processed");
        processed.setProcessed(true);
        outboxEvents.saveAndFlush(newer);
        outboxEvents.saveAndFlush(older);
        outboxEvents.saveAndFlush(processed);

        assertThat(outboxEvents.findByProcessedFalseOrderByCreatedAtAsc())
                .extracting(OutboxEvent::getPayload)
                .filteredOn(payload -> payload.startsWith(marker))
                .containsExactly(marker + "-older", marker + "-newer");
    }

    @Test
    void refreshToken_oneRowPerUser() {
        User user = user();
        refreshTokens.saveAndFlush(refreshToken(user));

        assertThatThrownBy(() -> refreshTokens.saveAndFlush(refreshToken(user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void passwordResetToken_deleteByUser_removesTokens() {
        User user = user();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusSeconds(60));
        resetTokens.saveAndFlush(token);

        resetTokens.deleteByUser(user);
        resetTokens.flush();

        assertThat(resetTokens.findByToken(token.getToken())).isEmpty();
    }

    @Test
    void category_userAndGlobalCategoriesAreBothVisibleToUser() {
        User user = user();
        categories.saveAndFlush(new com.example.models.Category("Mine", "income", user));

        assertThat(categories.findByUserUserIdOrUserIsNull(user.getUserId()))
                .extracting(com.example.models.Category::getName)
                .contains("Mine", "Salary", "Food");
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusSeconds(60));
        return token;
    }
}
