package com.example.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIT extends AbstractApiIntegrationTest {

    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(
            "testSecretKeyForTestingOnly1234567890abcdefghijklmnop".getBytes(StandardCharsets.UTF_8));
    private static final Key FOREIGN_KEY = Keys.hmacShaKeyFor(
            "aDifferentSecretKeyThatTheServerNeverIssued123456789".getBytes(StandardCharsets.UTF_8));

    static Stream<String> invalidTokens() {
        String valid = Jwts.builder().setSubject("victim").claim("role", "USER")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256).compact();
        String expired = Jwts.builder().setSubject("victim").claim("role", "USER")
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256).compact();
        String wrongKey = Jwts.builder().setSubject("victim").claim("role", "ADMIN")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(FOREIGN_KEY, SignatureAlgorithm.HS256).compact();
        String unsigned = Jwts.builder().setSubject("victim").claim("role", "ADMIN").compact();
        String tampered = valid.substring(0, valid.lastIndexOf('.') + 1) + "AAAA";
        return Stream.of(expired, wrongKey, unsigned, tampered, "not-a-jwt");
    }

    private ResponseEntity<String> settingsWithBearer(String token) {
        return call(HttpMethod.GET, "/api/settings", null, null,
                Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        return call(HttpMethod.POST, "/api/users/refresh", null, null,
                Map.of(HttpHeaders.COOKIE, "refresh_token=" + refreshToken));
    }

    @Test
    void register_validRequest_returns201WithoutPasswordInBody() {
        String username = "reg" + System.nanoTime();
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", username, "password", PASSWORD, "email", username + "@test.com"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(json(response).get("username").asText()).isEqualTo(username);
        assertThat(response.getBody()).doesNotContainIgnoringCase("password").doesNotContain("$2a$");
    }

    @Test
    void register_duplicateUsername_returns409() {
        ApiUser existing = newUser();
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", existing.username(), "password", PASSWORD, "email", "other" + System.nanoTime() + "@test.com"));

        assertErrorShape(response, 409);
        assertThat(json(response).get("message").asText()).isEqualTo("Username already taken");
    }

    @Test
    void register_duplicateEmail_returns409() {
        ApiUser existing = newUser();
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/register", null,
                Map.of("username", "other" + System.nanoTime(), "password", PASSWORD, "email", existing.email()));

        assertErrorShape(response, 409);
        assertThat(json(response).get("message").asText()).isEqualTo("Email already registered");
    }

    @Test
    void login_validCredentials_setsHardenedCookies() {
        ResponseEntity<String> response = login(newUser().username(), PASSWORD);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2).allSatisfy(cookie ->
                assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=None", "Path=/"));
    }

    @Test
    void login_wrongPasswordAndUnknownUser_areIndistinguishable() {
        ResponseEntity<String> wrongPassword = login(newUser().username(), "WrongPass1");
        ResponseEntity<String> unknownUser = login("nobody" + System.nanoTime(), "WrongPass1");

        assertErrorShape(wrongPassword, 401);
        assertErrorShape(unknownUser, 401);
        assertThat(json(wrongPassword).get("message")).isEqualTo(json(unknownUser).get("message"));
        assertThat(json(unknownUser).get("message").asText()).isEqualTo("Invalid username or password");
    }

    @Test
    void login_staleCookieOnPublicEndpoint_stillSucceeds() {
        ApiUser user = newUser();
        String expired = invalidTokens().findFirst().orElseThrow();

        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/login", null,
                Map.of("username", user.username(), "password", PASSWORD),
                Map.of(HttpHeaders.COOKIE, "auth_token=" + expired));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void bearerToken_isAcceptedLikeTheCookie() {
        assertThat(settingsWithBearer(newUser().token()).getStatusCode().value()).isEqualTo(200);
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void protectedEndpoint_invalidToken_isRejected(String token) {
        ResponseEntity<String> response = settingsWithBearer(token);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody() == null ? "" : response.getBody()).doesNotContain("primaryCurrency");
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void protectedEndpoint_invalidToken_returns401(String token) {
        assertThat(settingsWithBearer(token).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void refresh_noCookie_returns401() {
        assertThat(call(HttpMethod.POST, "/api/users/refresh", null, null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void refresh_unknownToken_returns401() {
        assertErrorShape(refresh("unknown-token"), 401);
    }

    @Test
    void refresh_expiredToken_returns401AndRemovesIt() {
        ApiUser user = newUser();
        String refreshToken = cookieValue(login(user.username(), PASSWORD), "refresh_token");
        jdbc.update("UPDATE refresh_tokens SET expiry_date = now() - interval '1 minute' WHERE user_id = ?", user.id());

        assertErrorShape(refresh(refreshToken), 401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ?", Integer.class, user.id()))
                .isZero();
    }

    @Test
    void refresh_rotatedToken_cannotBeReused() {
        String first = cookieValue(login(newUser().username(), PASSWORD), "refresh_token");
        ResponseEntity<String> rotated = refresh(first);

        assertThat(rotated.getStatusCode().value()).isEqualTo(200);
        assertThat(settingsWithBearer(cookieValue(rotated, "auth_token")).getStatusCode().value()).isEqualTo(200);
        assertErrorShape(refresh(first), 401);
    }

    @Test
    void logout_noCookies_returns204AndClearsCookies() {
        ResponseEntity<String> response = call(HttpMethod.POST, "/api/users/logout", null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
    }

    @Test
    void logout_invalidatesRefreshToken() {
        String refreshToken = cookieValue(login(newUser().username(), PASSWORD), "refresh_token");

        call(HttpMethod.POST, "/api/users/logout", null, null, Map.of(HttpHeaders.COOKIE, "refresh_token=" + refreshToken));

        assertErrorShape(refresh(refreshToken), 401);
    }

    @Disabled("BUG-15: logout only revokes the access token from the cookie, not from the Authorization header")
    @Test
    void logout_bearerToken_isRevoked() {
        ApiUser user = newUser();

        call(HttpMethod.POST, "/api/users/logout", null, null,
                Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + user.token()));

        assertThat(settingsWithBearer(user.token()).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void forgotPassword_unknownAndKnownEmail_returnSameResponse() {
        ResponseEntity<String> known = call(HttpMethod.POST, "/api/users/forgot-password", null,
                Map.of("email", newUser().email()));
        ResponseEntity<String> unknown = call(HttpMethod.POST, "/api/users/forgot-password", null,
                Map.of("email", "nobody" + System.nanoTime() + "@test.com"));

        assertThat(known.getStatusCode().value()).isEqualTo(200);
        assertThat(unknown.getStatusCode().value()).isEqualTo(200);
        assertThat(known.getBody()).isEqualTo(unknown.getBody());
    }

    @Test
    void resetPassword_validToken_changesPasswordAndKillsSessions() {
        ApiUser user = newUser();
        String refreshToken = cookieValue(login(user.username(), PASSWORD), "refresh_token");
        call(HttpMethod.POST, "/api/users/forgot-password", null, Map.of("email", user.email()));
        String resetToken = jdbc.queryForObject("SELECT token FROM password_reset_tokens WHERE user_id = ?",
                String.class, user.id());

        ResponseEntity<String> reset = call(HttpMethod.POST, "/api/users/reset-password", null,
                Map.of("token", resetToken, "newPassword", "NewPassw0rd"));

        assertThat(reset.getStatusCode().value()).isEqualTo(200);
        assertThat(login(user.username(), PASSWORD).getStatusCode().value()).isEqualTo(401);
        assertThat(login(user.username(), "NewPassw0rd").getStatusCode().value()).isEqualTo(200);
        assertErrorShape(refresh(refreshToken), 401);
        assertErrorShape(call(HttpMethod.POST, "/api/users/reset-password", null,
                Map.of("token", resetToken, "newPassword", "AnotherPassw0rd")), 401);
    }

    @Test
    void resetPassword_expiredToken_returns401() {
        ApiUser user = newUser();
        call(HttpMethod.POST, "/api/users/forgot-password", null, Map.of("email", user.email()));
        String resetToken = jdbc.queryForObject("SELECT token FROM password_reset_tokens WHERE user_id = ?",
                String.class, user.id());
        jdbc.update("UPDATE password_reset_tokens SET expiry_date = now() - interval '1 minute' WHERE user_id = ?", user.id());

        assertErrorShape(call(HttpMethod.POST, "/api/users/reset-password", null,
                Map.of("token", resetToken, "newPassword", "NewPassw0rd")), 401);
        assertThat(login(user.username(), PASSWORD).getStatusCode().value()).isEqualTo(200);
    }
}
