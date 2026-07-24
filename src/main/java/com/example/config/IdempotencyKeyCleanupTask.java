package com.example.config;

import com.example.repository.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class IdempotencyKeyCleanupTask {

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    // Runs every hour and deletes keys older than 48 hours (no replay/duplicate risk remains)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeOldKeys() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        idempotencyKeyRepository.deleteByCreatedAtBefore(cutoff);
    }
}