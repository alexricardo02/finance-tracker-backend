package com.example.config;

import com.example.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyCleanupTaskTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyKeyCleanupTask cleanupTask;

    @Test
    void purgeOldKeys_calculates48HourCutoffAndInvokesRepository() {
        Instant beforeCall = Instant.now().minus(48, ChronoUnit.HOURS);

        cleanupTask.purgeOldKeys();

        Instant afterCall = Instant.now().minus(48, ChronoUnit.HOURS);

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(idempotencyKeyRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());

        Instant actualCutoff = cutoffCaptor.getValue();
        assertThat(actualCutoff).isBetween(beforeCall.minus(Duration.ofSeconds(1)), afterCall.plus(Duration.ofSeconds(1)));
    }

    @Test
    void purgeOldKeys_whenRepositoryThrows_propagatesException() {
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        doThrow(new RuntimeException("Database error during purge"))
                .when(idempotencyKeyRepository).deleteByCreatedAtBefore(cutoffCaptor.capture());

        assertThatThrownBy(() -> cleanupTask.purgeOldKeys())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during purge");
    }
}
