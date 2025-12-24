package com.example.service;

import com.example.dataTransferObjects.ExchangeApiResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;


import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final WebClient webClient;
    private final Cache<String, CachedRates> cache;

    // Lista de monedas que aceptas en la app (ajusta según necesites)
    private static final List<String> SUPPORTED_CURRENCIES = List.of("EUR","USD","GBP","JPY","AUD","CAD","CHF");

    public ExchangeRateService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.exchangerate.host").build();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5_000)
                .build();
    }

    private static class CachedRates {
        final Map<String, BigDecimal> rates; // rates relative to a base: 1 BASE = rates.get(currency)
        final Instant fetchedAt;
        CachedRates(Map<String, BigDecimal> rates, Instant fetchedAt) {
            this.rates = rates;
            this.fetchedAt = fetchedAt;
        }
    }

    /**
     * PREFETCH al iniciar: solicitamos /latest?base=EUR&symbols=... y lo guardamos en cache con clave "EUR|latest".
     * @PostConstruct no acepta parámetros.
     */
    @PostConstruct
    public void prefetchSupportedCurrenciesLatest() {
        try {
            prefetchBaseForDate("EUR", null);
            log.info("Prefetch latest rates for EUR done.");
        } catch (Exception e) {
            log.warn("Prefetch failed: {}", e.toString());
        }
    }

    /**
     * (Opcional) refresco programado cada hora para mantener la cache caliente.
     * Ajusta el cron/fixedRate según tus necesidades.
     */
    @Scheduled(fixedDelayString = "${exchange.cache.refresh-ms:3600000}") // 1 h por defecto
    public void scheduledRefreshLatest() {
        try {
            prefetchSupportedCurrenciesLatest();
            log.info("Scheduled refresh of latest rates finished.");
        } catch (Exception e) {
            log.warn("Scheduled refresh failed: {}", e.toString());
        }
    }

    /**
     * Prefetch helper: pide rates para una base y fecha (date == null -> latest).
     */
    public void prefetchBaseForDate(String base, LocalDate date) {
        String dateKey = (date == null) ? "latest" : date.toString();
        String cacheKey = base.toUpperCase() + "|" + dateKey;

        // construimos lista de symbols: todas las soportadas excepto la base misma (si base==EUR incluye USD,...)
        List<String> symbolsList = SUPPORTED_CURRENCIES.stream()
                .map(String::toUpperCase)
                .filter(s -> !s.equals(base.toUpperCase()))
                .collect(Collectors.toList());
        String symbolsParam = String.join(",", symbolsList);

        String path = (date == null) ? "/latest" : "/" + dateKey;
        ExchangeApiResponse resp = webClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("base", base.toUpperCase())
                        .queryParam("symbols", symbolsParam)
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        Mono.error(new RuntimeException("Error fetching rates: " + clientResponse.statusCode())))
                .bodyToMono(ExchangeApiResponse.class)
                .block(Duration.ofSeconds(10));

        if (resp == null || resp.getRates() == null) {
            throw new RuntimeException("No rates in API response for base=" + base + " date=" + dateKey);
        }

        // Convertir Map<String,Double> a Map<String,BigDecimal> y añadir la propia base con valor 1
        Map<String, BigDecimal> ratesMap = new HashMap<>();
        resp.getRates().forEach((k,v) -> ratesMap.put(k.toUpperCase(), BigDecimal.valueOf(v)));
        ratesMap.put(base.toUpperCase(), BigDecimal.ONE);

        cache.put(cacheKey, new CachedRates(ratesMap, Instant.now()));
        log.info("Cached rates for {}|{}", base, dateKey);
    }

    /**
     * Obtiene la tasa from -> to para la fecha dada (date == null -> latest).
     * Estrategia: intenta usar cache de base EUR (o de la base solicitada si existe),
     * y calcula rate = ratesBase.get(to) / ratesBase.get(from).
     */
    /*
    public BigDecimal getRate(String from, String to, LocalDate date) {
        from = from.toUpperCase();
        to = to.toUpperCase();
        String dateKey = (date == null) ? "latest" : date.toString();

        // Primero intentamos usar cache para base EUR (porque la precargamos)
        String baseKey = "EUR|" + dateKey;
        CachedRates cached = cache.getIfPresent(baseKey);

        if (cached != null && cached.rates.containsKey(from) && cached.rates.containsKey(to)) {
            BigDecimal rFrom = cached.rates.get(from);
            BigDecimal rTo = cached.rates.get(to);
            // rate from->to = rTo / rFrom
            return rTo.divide(rFrom, 12, RoundingMode.HALF_UP);
        }

        // Si no tenemos cache para EUR/date, intentamos prefetchear base EUR para esa fecha
        try {
            prefetchBaseForDate("EUR", date);
            cached = cache.getIfPresent(baseKey);
            if (cached != null && cached.rates.containsKey(from) && cached.rates.containsKey(to)) {
                BigDecimal rFrom = cached.rates.get(from);
                BigDecimal rTo = cached.rates.get(to);
                return rTo.divide(rFrom, 12, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("Prefetch failed: {}", e.toString());
            // seguiremos para fallback más abajo
        }

        // Fallback: si API soporta base param, pedimos directamente pair from -> to (may be costlier)
        try {
            String path = (date == null) ? "/latest" : "/" + dateKey;
            ExchangeApiResponse resp = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(path)
                            .queryParam("base", from)
                            .queryParam("symbols", to)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            Mono.error(new RuntimeException("Error fetching rate pair: " + clientResponse.statusCode())))
                    .bodyToMono(ExchangeApiResponse.class)
                    .block(Duration.ofSeconds(10));

            if (resp != null && resp.getRates() != null && resp.getRates().containsKey(to)) {
                Double val = resp.getRates().get(to);
                BigDecimal rate = BigDecimal.valueOf(val);
                // cacheamos el resultado mínimo (para base=from,date)
                Map<String, BigDecimal> smallMap = new HashMap<>();
                smallMap.put(to, rate);
                smallMap.put(from, BigDecimal.ONE);
                cache.put(from + "|" + dateKey, new CachedRates(smallMap, Instant.now()));
                return rate;
            }
        } catch (Exception ex) {
            log.warn("Direct pair fetch failed: {}", ex.toString());
        }

        throw new RuntimeException("Unable to obtain rate for " + from + " -> " + to + " date=" + dateKey);
    }
    */
    /** Convierte amount from->to usando getRate y redondea a 2 decimales. */
    /*public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date) {
        if (from.equalsIgnoreCase(to)) return amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = getRate(from, to, date);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    */
    /* Helper para extraer código de moneda de strings como "$ USD" o "€ EUR". */
    /*public static String extractCurrencyCode(String input) {
        if (input == null) return null;
        input = input.trim();
        // caso simple: si termina en 'XXX' (3 letras) lo devolvemos
        String[] parts = input.split("\\s+");
        String last = parts[parts.length - 1];
        if (last.matches("[A-Z]{3}")) return last;
        // fallback por símbolos
        if (input.startsWith("€")) return "EUR";
        if (input.startsWith("$")) {
            // Ambigüedad: $ puede ser USD, AUD o CAD. Mejor que el usuario guarde el código.
            return "USD";
        }
        if (input.startsWith("£")) return "GBP";
        if (input.startsWith("¥")) return "JPY";
        return last.toUpperCase();
    }
    */
    

}