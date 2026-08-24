package com.example.controllers;

import com.example.dataTransferObjects.ExchangeRateResponseDTO;
import com.example.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks private ExchangeRateController exchangeRateController;

    @Test
    void getUsdArsOficial_delegatesToService_returnsServiceResult() {
        ExchangeRateResponseDTO dto = new ExchangeRateResponseDTO();
        dto.setCompra(1050.0);
        dto.setVenta(1090.0);
        dto.setCasa("oficial");
        dto.setFechaActualizacion("2026-07-01T12:00:00.000Z");
        when(exchangeRateService.getOficialRate()).thenReturn(dto);

        ExchangeRateResponseDTO result = exchangeRateController.getUsdArsOficial();

        assertThat(result.getVenta()).isEqualTo(1090.0);
        assertThat(result.getCasa()).isEqualTo("oficial");
        verify(exchangeRateService).getOficialRate();
    }

    @Test
    void getUsdArsOficial_serviceFallback_returnsZeroedFallbackDTO() {
        // WHY: verifies the controller transparently returns the circuit-breaker
        // fallback DTO when the external dolarapi.com is unavailable.
        ExchangeRateResponseDTO fallback = new ExchangeRateResponseDTO();
        fallback.setCompra(0.0);
        fallback.setVenta(0.0);
        fallback.setCasa("fallback");
        fallback.setFechaActualizacion("unavailable");
        when(exchangeRateService.getOficialRate()).thenReturn(fallback);

        ExchangeRateResponseDTO result = exchangeRateController.getUsdArsOficial();

        assertThat(result.getCasa()).isEqualTo("fallback");
        assertThat(result.getVenta()).isEqualTo(0.0);
    }
}
