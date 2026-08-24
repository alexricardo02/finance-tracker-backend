package com.example.integration;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full auth flow integration test: register → login → refresh → logout → blacklist check.
 *
 * Uses a real Postgres + Redis (Testcontainers) — no mocks for infrastructure.
 * Only external services (mail, exchange-rate APIs) are stubbed via property override.
 */
class AuthFlowIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/users";
    }

    @Test
    void fullAuthFlow_registerLoginRefreshLogout() throws Exception {
        // ── 1. Register ──────────────────────────────────────────────────────
        UserRegistrationDTO registration = new UserRegistrationDTO(
                "ituser_" + System.nanoTime(), "Passw0rd!", "ituser_" + System.nanoTime() + "@test.com");

        ResponseEntity<String> registerResp = restTemplate.postForEntity(
                baseUrl() + "/register",
                toJsonEntity(registration),
                String.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ── 2. Login — assert httpOnly cookies are set ────────────────────────
        LoginRequestDTO login = new LoginRequestDTO(
                registration.getUsername(), registration.getPassword());

        ResponseEntity<String> loginResp = restTemplate.postForEntity(
                baseUrl() + "/login",
                toJsonEntity(login),
                String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> setCookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull().hasSizeGreaterThanOrEqualTo(2);

        String authCookieHeader = extractCookieHeader(setCookies, "auth_token");
        String refreshCookieHeader = extractCookieHeader(setCookies, "refresh_token");
        assertThat(authCookieHeader).isNotNull().contains("HttpOnly");
        assertThat(refreshCookieHeader).isNotNull().contains("HttpOnly");

        String authToken     = extractCookieValue(authCookieHeader, "auth_token");
        String refreshToken  = extractCookieValue(refreshCookieHeader, "refresh_token");

        // ── 3. Access a protected endpoint with the access token ──────────────
        HttpHeaders protectedHeaders = new HttpHeaders();
        protectedHeaders.add(HttpHeaders.COOKIE, "auth_token=" + authToken);

        ResponseEntity<String> settingsResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/settings",
                HttpMethod.GET,
                new HttpEntity<>(protectedHeaders),
                String.class);
        assertThat(settingsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ── 4. Refresh — token rotation: old refresh token → new pair ─────────
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "refresh_token=" + refreshToken);

        ResponseEntity<String> refreshResp = restTemplate.exchange(
                baseUrl() + "/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshHeaders),
                String.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> newCookies = refreshResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(newCookies).isNotNull().hasSizeGreaterThanOrEqualTo(2);

        String newAuthToken = extractCookieValue(extractCookieHeader(newCookies, "auth_token"), "auth_token");
        String newRefreshToken = extractCookieValue(extractCookieHeader(newCookies, "refresh_token"), "refresh_token");

        // WHY: Rotated refresh token must be different; reuse of the old one must fail.
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // ── 5. Old refresh token must be rejected after rotation ──────────────
        HttpHeaders oldRefreshHeaders = new HttpHeaders();
        oldRefreshHeaders.add(HttpHeaders.COOKIE, "refresh_token=" + refreshToken);

        ResponseEntity<String> reuseOldResp = restTemplate.exchange(
                baseUrl() + "/refresh",
                HttpMethod.POST,
                new HttpEntity<>(oldRefreshHeaders),
                String.class);
        // The old token was rotated, so it no longer exists in the DB → 5xx or 401
        assertThat(reuseOldResp.getStatusCode().is5xxServerError()
                || reuseOldResp.getStatusCode() == HttpStatus.UNAUTHORIZED).isTrue();

        // ── 6. Logout — blacklist the access token ────────────────────────────
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.add(HttpHeaders.COOKIE, "auth_token=" + newAuthToken);
        logoutHeaders.add(HttpHeaders.COOKIE, "refresh_token=" + newRefreshToken);

        ResponseEntity<Void> logoutResp = restTemplate.exchange(
                baseUrl() + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutHeaders),
                Void.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // ── 7. Blacklisted access token must be rejected ──────────────────────
        HttpHeaders blacklistedHeaders = new HttpHeaders();
        blacklistedHeaders.add(HttpHeaders.COOKIE, "auth_token=" + newAuthToken);

        ResponseEntity<String> afterLogoutResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/settings",
                HttpMethod.GET,
                new HttpEntity<>(blacklistedHeaders),
                String.class);
        assertThat(afterLogoutResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpEntity<String> toJsonEntity(Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
    }

    private String extractCookieHeader(List<String> setCookieHeaders, String cookieName) {
        return setCookieHeaders.stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst()
                .orElse(null);
    }

    private String extractCookieValue(String cookieHeader, String cookieName) {
        if (cookieHeader == null) return null;
        String prefix = cookieName + "=";
        int start = cookieHeader.indexOf(prefix) + prefix.length();
        int end = cookieHeader.indexOf(';', start);
        return end == -1 ? cookieHeader.substring(start) : cookieHeader.substring(start, end);
    }
}
