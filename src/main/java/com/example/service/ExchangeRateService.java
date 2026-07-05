package com.example.service;

import com.example.dataTransferObjects.ExchangeRateResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private final RestClient restClient = RestClient.create("https://dolarapi.com");

    // WHY: cached 1h via RedisConfig defaults to avoid hammering the free public API
    @Cacheable(value = "usd_ars_oficial", key = "'rate'")
    public ExchangeRateResponseDTO getOficialRate() {
        try {
            return restClient.get()
                    .uri("/v1/dolares/oficial")
                    .retrieve()
                    .body(ExchangeRateResponseDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch USD/ARS official rate", e);
            throw new RuntimeException("Exchange rate service unavailable");
        }
    }
}
