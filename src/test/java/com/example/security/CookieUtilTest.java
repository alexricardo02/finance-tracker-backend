package com.example.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    private final CookieUtil cookieUtil = new CookieUtil();

    @Test
    void buildAccessTokenCookie_isHttpOnlySecureSameSiteNone() {
        ResponseCookie cookie = cookieUtil.buildAccessTokenCookie("jwt-token", 900);

        assertThat(cookie.getName()).isEqualTo("auth_token");
        assertThat(cookie.getValue()).isEqualTo("jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(900);
    }

    @Test
    void buildRefreshTokenCookie_isHttpOnlySecureSameSiteNone() {
        ResponseCookie cookie = cookieUtil.buildRefreshTokenCookie("refresh-token", 604800);

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800);
    }

    @Test
    void clearCookie_hasZeroMaxAgeAndEmptyValue_butKeepsSecurityFlags() {
        ResponseCookie cookie = cookieUtil.clearCookie("auth_token");

        assertThat(cookie.getName()).isEqualTo("auth_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
    }
}