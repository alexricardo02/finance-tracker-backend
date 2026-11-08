package com.example.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateClientConfigTest {

    private final ExchangeRateClientConfig config = new ExchangeRateClientConfig();

    private Object readTimeoutOf(RestClient client) {
        Object requestFactory = ReflectionTestUtils.getField(client, "clientRequestFactory");
        return ReflectionTestUtils.getField(requestFactory, "readTimeout");
    }

    @Disabled("BUG-12: FX RestClients have no read timeout")
    @Test
    void fxClients_haveAReadTimeout() {
        assertThat(readTimeoutOf(config.dolarApiClient())).isNotNull();
        assertThat(readTimeoutOf(config.frankfurterClient())).isNotNull();
    }
}
