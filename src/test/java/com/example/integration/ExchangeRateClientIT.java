package com.example.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateClientIT extends AbstractApiIntegrationTest {

    private static final String DOLAR_PATH = "/v1/dolares/oficial";

    private ResponseEntity<String> usdArs() {
        return call(HttpMethod.GET, "/api/exchange-rate/usd-ars", newUser(), null);
    }

    private void assertFallback(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("casa").asText()).isEqualTo("fallback");
    }

    private List<Double> storedPrimaryAmounts(ApiUser user) {
        return jdbc.queryForList("SELECT amount_primary_currency FROM incomes WHERE user_id = ?", Double.class, user.id());
    }

    @Test
    void usdArs_upstreamOk_returnsMappedRate() {
        stubDolarOficial(1050.5);

        JsonNode body = json(usdArs());

        assertThat(body.get("venta").asDouble()).isEqualTo(1050.5);
        assertThat(body.get("compra").asDouble()).isEqualTo(1040.5);
        assertThat(body.get("casa").asText()).isEqualTo("oficial");
    }

    @Test
    void usdArs_upstream500_returnsFallback() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));

        assertFallback(usdArs());
    }

    @Disabled("BUG-14: the circuit breaker fallback runs inside Retry, so failed calls are never retried")
    @Test
    void usdArs_upstream500_isRetriedThreeTimes() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));

        usdArs();

        FX_API.verify(3, getRequestedFor(urlPathEqualTo(DOLAR_PATH)));
    }

    @Test
    void usdArs_malformedBody_returnsFallback() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(okJson("<html>gateway error</html>")));

        assertFallback(usdArs());
    }

    @Test
    void usdArs_slowUpstreamPastClientTimeout_returnsFallback() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(okJson("{}").withFixedDelay(2500)));

        assertFallback(usdArs());
    }

    @Test
    void usdArs_connectionDropped_returnsFallback() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        assertFallback(usdArs());
    }

    @Test
    void circuitBreaker_opensAfterRepeatedFailures_andStopsCallingUpstream() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();

        for (int i = 0; i < 10; i++) {
            assertFallback(call(HttpMethod.GET, "/api/exchange-rate/usd-ars", user, null));
        }

        assertThat(FX_API.findAll(getRequestedFor(urlPathEqualTo(DOLAR_PATH))).size()).isLessThan(10);
    }

    @Test
    void conversion_nonArsPair_usesHistoricalRateOfTransactionDate() {
        stubFrankfurter("EUR", "USD", "2026-03-15", 1.2);

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", newUser(),
                incomeBody(100, "EUR", "2026-03-15", "x"));

        assertThat(json(response).get("amountPrimaryCurrency").asDouble()).isCloseTo(120.0, org.assertj.core.data.Offset.offset(1e-9));
        FX_API.verify(getRequestedFor(urlPathEqualTo("/v1/2026-03-15"))
                .withQueryParam("base", equalTo("EUR")).withQueryParam("symbols", equalTo("USD")));
    }

    @Test
    void conversion_arsToUsd_usesDolarVenta() {
        stubDolarOficial(1000.0);

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", newUser(), incomeBody(5000, "ARS", TX_DATE, "x"));

        assertThat(json(response).get("amountPrimaryCurrency").asDouble()).isEqualTo(5.0);
    }

    @Test
    void conversion_arsToEur_bridgesThroughUsd() {
        stubDolarOficial(1000.0);
        stubFrankfurter("USD", "EUR", TX_DATE, 0.5);
        ApiUser user = newUser();
        jdbc.update("UPDATE users SET primary_currency = 'EUR' WHERE user_id = ?", user.id());

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", user, incomeBody(4000, "ARS", TX_DATE, "x"));

        assertThat(json(response).get("amountPrimaryCurrency").asDouble()).isEqualTo(2.0);
    }

    @Test
    void conversion_frankfurterDown_stillSavesTransaction() {
        FX_API.stubFor(get(urlPathEqualTo("/v1/" + TX_DATE)).willReturn(serverError()));
        ApiUser user = newUser();

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "EUR", TX_DATE, "x"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(storedPrimaryAmounts(user)).containsExactly(100.0);
    }

    @Test
    void conversion_frankfurterFallback_isNotCachedAfterRecovery() {
        FX_API.stubFor(get(urlPathEqualTo("/v1/" + TX_DATE)).willReturn(serverError()));
        ApiUser user = newUser();
        call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "EUR", TX_DATE, "during outage"));

        FX_API.resetAll();
        stubFrankfurter("EUR", "USD", TX_DATE, 2.0);
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "EUR", TX_DATE, "after recovery"));

        assertThat(json(response).get("amountPrimaryCurrency").asDouble()).isEqualTo(200.0);
    }

    @Test
    void conversion_arsToUsd_dolarApiDown_neverStoresInfinityOrZero() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(5000, "ARS", TX_DATE, "x")), 503);

        assertThat(storedPrimaryAmounts(user)).isEmpty();
    }

    @Test
    void conversion_usdToArs_dolarApiDown_neverStoresZero() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();
        jdbc.update("UPDATE users SET primary_currency = 'ARS' WHERE user_id = ?", user.id());

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "USD", TX_DATE, "x")), 503);

        assertThat(storedPrimaryAmounts(user)).isEmpty();
    }

    @Test
    void conversion_arsBridge_frankfurterDown_isRefused() {
        stubDolarOficial(1000.0);
        FX_API.stubFor(get(urlPathEqualTo("/v1/" + TX_DATE)).willReturn(serverError()));
        ApiUser user = newUser();
        jdbc.update("UPDATE users SET primary_currency = 'EUR' WHERE user_id = ?", user.id());

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(4000, "ARS", TX_DATE, "x")), 503);

        assertThat(storedPrimaryAmounts(user)).isEmpty();
    }

    @Test
    void conversion_dolarApiReturnsNoRate_isRefused() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(okJson("{}")));

        assertErrorShape(call(HttpMethod.POST, "/api/incomes", newUser(), incomeBody(5000, "ARS", TX_DATE, "x")), 503);
    }

    @Test
    void usdArs_fallback_isNotCachedAfterRecovery() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        assertFallback(usdArs());

        FX_API.resetAll();
        stubDolarOficial(1000.0);

        assertThat(json(usdArs()).get("venta").asDouble()).isEqualTo(1000.0);
    }

    @Test
    void arsConversion_recoversAfterDolarApiOutage() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();
        assertErrorShape(call(HttpMethod.POST, "/api/incomes", user, incomeBody(5000, "ARS", TX_DATE, "x")), 503);

        FX_API.resetAll();
        stubDolarOficial(1000.0);

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/incomes", user, incomeBody(5000, "ARS", TX_DATE, "x"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(storedPrimaryAmounts(user)).containsExactly(5.0);
    }

    @Test
    void currencyChange_toArsDuringOutage_isRefusedAndChangesNothing() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();
        createIncome(user, 100, "x");

        assertErrorShape(call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "ARS")), 503);

        assertThat(jdbc.queryForObject("SELECT primary_currency FROM users WHERE user_id = ?", String.class, user.id())).isEqualTo("USD");
        assertThat(storedPrimaryAmounts(user)).containsExactly(100.0);
    }

    @Test
    void importDuringOutage_skipsArsRowsWithAReason() {
        FX_API.stubFor(get(urlPathEqualTo(DOLAR_PATH)).willReturn(serverError()));
        ApiUser user = newUser();
        String rows = "[{\"kind\":\"income\",\"amount\":5.0,\"currency\":\"USD\",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"Salary\"},"
                + "{\"kind\":\"income\",\"amount\":5000.0,\"currency\":\"ARS\",\"date\":\"" + TX_DATE + "\",\"categoryName\":\"Salary\"}]";

        JsonNode result = json(call(HttpMethod.POST, "/api/imports/transactions", user, rows));

        assertThat(result.get("imported").asInt()).isEqualTo(1);
        assertThat(result.get("skipped").asInt()).isEqualTo(1);
        assertThat(storedPrimaryAmounts(user)).containsExactly(5.0);
    }

    @Test
    void currencyChange_fxRequestCarriesEachTransactionsOwnDate() {
        stubFrankfurter("USD", "EUR", "2026-01-10", 0.5);
        stubFrankfurter("USD", "EUR", "2026-02-20", 0.25);
        ApiUser user = newUser();
        call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "USD", "2026-01-10", "a"));
        call(HttpMethod.POST, "/api/incomes", user, incomeBody(100, "USD", "2026-02-20", "b"));

        call(HttpMethod.PUT, "/api/settings/currency", user, Map.of("primaryCurrency", "EUR"));

        assertThat(storedPrimaryAmounts(user)).containsExactlyInAnyOrder(50.0, 25.0);
    }
}
