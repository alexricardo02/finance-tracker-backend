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

    // Corre cada hora, borra keys con más de 48h (ya no hay riesgo de replay/duplicado)
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeOldKeys() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        idempotencyKeyRepository.deleteByCreatedAtBefore(cutoff);
    }
}