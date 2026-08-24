package com.example.service;

import com.example.dataTransferObjects.ExchangeRateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private RestClient dolarApiClient;
    @Mock private RestClient frankfurterClient;

    @Mock private RestClient.RequestHeadersUriSpec dolarUriSpec;
    @Mock private RestClient.RequestHeadersSpec dolarHeadersSpec;
    @Mock private RestClient.ResponseSpec dolarResponseSpec;

    @Mock private RestClient.RequestHeadersUriSpec frankfurterUriSpec;
    @Mock private RestClient.RequestHeadersSpec frankfurterHeadersSpec;
    @Mock private RestClient.ResponseSpec frankfurterResponseSpec;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(dolarApiClient, frankfurterClient);
        // WHY: 'self' bypasses the Spring AOP proxy (@CircuitBreaker/@Retry) in unit tests,
        // so we point it back at the real instance to exercise the plain method logic.
        ReflectionTestUtils.setField(exchangeRateService, "self", exchangeRateService);
    }

    @SuppressWarnings("unchecked")
    private void mockDolarApi(double venta) {
        when(dolarApiClient.get()).thenReturn(dolarUriSpec);
        when(dolarUriSpec.uri("/v1/dolares/oficial")).thenReturn(dolarHeadersSpec);
        when(dolarHeadersSpec.retrieve()).thenReturn(dolarResponseSpec);
        ExchangeRateResponseDTO dto = new ExchangeRateResponseDTO();
        dto.setCompra(venta - 10);
        dto.setVenta(venta);
        dto.setCasa("oficial");
        dto.setFechaActualizacion("2026-07-25");
        when(dolarResponseSpec.body(ExchangeRateResponseDTO.class)).thenReturn(dto);
    }

    @SuppressWarnings("unchecked")
    private void mockFrankfurter(double rate, String to) {
        when(frankfurterClient.get()).thenReturn(frankfurterUriSpec);
        when(frankfurterUriSpec.uri(anyString())).thenReturn(frankfurterHeadersSpec);
        when(frankfurterHeadersSpec.retrieve()).thenReturn(frankfurterResponseSpec);
        when(frankfurterResponseSpec.body(Map.class)).thenReturn(Map.of("rates", Map.of(to, rate)));
    }

    @Test
    void getConversionRate_sameCurrency_returnsOne_noNetworkCall() {
        double rate = exchangeRateService.getConversionRate("USD", "USD", LocalDate.now());

        assertThat(rate).isEqualTo(1.0);
        verifyNoInteractions(dolarApiClient, frankfurterClient);
    }

    @Test
    void getConversionRate_nullCurrencies_returnsOne() {
        assertThat(exchangeRateService.getConversionRate(null, "USD", LocalDate.now())).isEqualTo(1.0);
        assertThat(exchangeRateService.getConversionRate("USD", null, LocalDate.now())).isEqualTo(1.0);
    }

    @Test
    void getConversionRate_usdToArs_usesDolarApiVenta() {
        mockDolarApi(1000.0);

        double rate = exchangeRateService.getConversionRate("USD", "ARS", LocalDate.now());

        assertThat(rate).isEqualTo(1000.0);
    }

    @Test
    void getConversionRate_arsToUsd_isInverseOfVenta() {
        mockDolarApi(1000.0);

        double rate = exchangeRateService.getConversionRate("ARS", "USD", LocalDate.now());

        assertThat(rate).isEqualTo(1.0 / 1000.0);
    }

    @Test
    void getConversionRate_eurToUsd_usesFrankfurterHistoricalRate() {
        mockFrankfurter(1.08, "USD");

        double rate = exchangeRateService.getConversionRate("EUR", "USD", LocalDate.of(2026, 7, 1));

        assertThat(rate).isEqualTo(1.08);
    }

    @Test
    void getOficialRate_success_mapsResponse() {
        mockDolarApi(1050.5);

        ExchangeRateResponseDTO result = exchangeRateService.getOficialRate();

        assertThat(result.getVenta()).isEqualTo(1050.5);
    }

    @Test
    void getOficialRate_apiFails_throwsRuntimeException() {
        when(dolarApiClient.get()).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> exchangeRateService.getOficialRate())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void getOficialRateFallback_returnsSafeConservativeValue_neverNull() {
        ExchangeRateResponseDTO fallback = exchangeRateService.getOficialRateFallback(new RuntimeException("circuit open"));

        assertThat(fallback.getCompra()).isEqualTo(0.0);
        assertThat(fallback.getVenta()).isEqualTo(0.0);
        assertThat(fallback.getCasa()).isEqualTo("fallback");
    }

    @Test
    void getRateFromFrankfurterFallback_returnsOne_noOpConversion() {
        double result = exchangeRateService.getRateFromFrankfurterFallback(
                "EUR", "USD", LocalDate.now(), new RuntimeException("timeout"));

        assertThat(result).isEqualTo(1.0);
    }
}