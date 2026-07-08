package com.example.service;

import com.example.dataTransferObjects.ExchangeRateResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private final RestClient dolarApiClient = RestClient.create("https://dolarapi.com");
    private final RestClient frankfurterClient = RestClient.create("https://api.frankfurter.dev");

    @Cacheable(value = "usd_ars_oficial", key = "'rate'")
    @CircuitBreaker(name = "exchangeRate", fallbackMethod = "getOficialRateFallback")
    @Retry(name = "exchangeRate")
    public ExchangeRateResponseDTO getOficialRate() {
        try {
            return dolarApiClient.get()
                    .uri("/v1/dolares/oficial")
                    .retrieve()
                    .body(ExchangeRateResponseDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch USD/ARS official rate", e);
            throw new RuntimeException("Exchange rate service unavailable");
        }
    }
    
 // Fallback: NO tirar 500. Devuelve el último valor válido o un valor conservador.
    private ExchangeRateResponseDTO getOficialRateFallback(Throwable t) {
        log.warn("Circuit breaker OPEN para DolarAPI. Usando fallback. Causa: {}", t.getMessage());
        ExchangeRateResponseDTO fallback = new ExchangeRateResponseDTO();
        fallback.setCompra(0.0);
        fallback.setVenta(0.0);
        fallback.setCasa("fallback");
        fallback.setFechaActualizacion("unavailable");
        return fallback;
    }

    /**
     * Units of toCurrency per 1 unit of fromCurrency, as of `date`.
     * WHY: each transaction stores the rate of ITS OWN date so re-converting the
     * whole history later (e.g. on primary-currency change) stays accurate,
     * instead of applying today's rate to old transactions.
     */
    @Cacheable(value = "fx_rate", key = "#fromCurrency + '_' + #toCurrency + '_' + #date")
    public double getConversionRate(String fromCurrency, String toCurrency, LocalDate date) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return 1.0;
        }
        if ("ARS".equalsIgnoreCase(fromCurrency) || "ARS".equalsIgnoreCase(toCurrency)) {
            return getRateViaArsBridge(fromCurrency, toCurrency, date);
        }
        return getRateFromFrankfurter(fromCurrency, toCurrency, date);
    }

    // WHY: DolarAPI only exposes the CURRENT official rate (no historical endpoint),
    // an accepted limitation for ARS pairs specifically. Every other pair uses
    // Frankfurter's historical ECB data.
    private double getRateViaArsBridge(String fromCurrency, String toCurrency, LocalDate date) {
        double arsPerUsd = getOficialRate().getVenta();
        if ("ARS".equalsIgnoreCase(fromCurrency) && "USD".equalsIgnoreCase(toCurrency)) return 1.0 / arsPerUsd;
        if ("USD".equalsIgnoreCase(fromCurrency) && "ARS".equalsIgnoreCase(toCurrency)) return arsPerUsd;

        if ("ARS".equalsIgnoreCase(fromCurrency)) {
            double usdToTarget = getRateFromFrankfurter("USD", toCurrency, date);
            return (1.0 / arsPerUsd) * usdToTarget;
        } else {
            double sourceToUsd = getRateFromFrankfurter(fromCurrency, "USD", date);
            return sourceToUsd * arsPerUsd;
        }
    }

    @CircuitBreaker(name = "exchangeRate", fallbackMethod = "getRateFromFrankfurterFallback")
    @Retry(name = "exchangeRate")
    @SuppressWarnings("unchecked")
    public double getRateFromFrankfurter(String fromCurrency, String toCurrency, LocalDate date) {
        try {
            String path = "/v1/" + date + "?base=" + fromCurrency + "&symbols=" + toCurrency;
            Map<String, Object> response = frankfurterClient.get().uri(path).retrieve().body(Map.class);
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            return ((Number) rates.get(toCurrency)).doubleValue();
        } catch (Exception e) {
            log.error("Failed to fetch FX rate {} -> {} on {}", fromCurrency, toCurrency, date, e);
            throw new RuntimeException("Exchange rate service unavailable for " + fromCurrency + "->" + toCurrency);
        }
    }
    
	 // Fallback: usa 1.0 (no-op conversion) para NO bloquear el guardado del income/expense.
	 // El usuario prefiere guardar con una tasa desactualizada a no poder guardar nada.
	 public double getRateFromFrankfurterFallback(String fromCurrency, String toCurrency, LocalDate date, Throwable t) {
	     log.warn("Circuit breaker OPEN para Frankfurter ({} -> {}). Fallback = 1.0. Causa: {}",
	             fromCurrency, toCurrency, t.getMessage());
	     return 1.0;
	 }
}