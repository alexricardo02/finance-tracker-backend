package com.example.security;

import com.example.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test_secret_key_for_unit_tests_1234567890");
        jwtUtil.init();
    }

    @Test
    void generateToken_and_extractUsername_returnsSameUsername() {
        String token = jwtUtil.generateToken("john", Role.USER);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void generateToken_and_extractRole_returnsSameRole() {
        String token = jwtUtil.generateToken("admin", Role.ADMIN);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("john", Role.USER);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.valid.token")).isFalse();
    }

    @Test
    void isTokenValid_tokenSignedWithDifferentKey_returnsFalse() {
        // WHY: guards against key-confusion / forged tokens signed with an attacker's key.
        JwtUtil otherIssuer = new JwtUtil();
        ReflectionTestUtils.setField(otherIssuer, "secret", "a_completely_different_secret_key_1234567890");
        otherIssuer.init();

        String forgedToken = otherIssuer.generateToken("john", Role.ADMIN);

        assertThat(jwtUtil.isTokenValid(forgedToken)).isFalse();
    }

    @Test
    void extractJti_isPresent_andUniquePerToken() {
        String token1 = jwtUtil.generateToken("john", Role.USER);
        String token2 = jwtUtil.generateToken("john", Role.USER);

        assertThat(jwtUtil.extractJti(token1)).isNotBlank();
        assertThat(jwtUtil.extractJti(token1)).isNotEqualTo(jwtUtil.extractJti(token2));
    }

    @Test
    void getRemainingValiditySeconds_freshToken_isCloseToFullExpiration() {
        String token = jwtUtil.generateToken("john", Role.USER);

        long remaining = jwtUtil.getRemainingValiditySeconds(token);

        assertThat(remaining).isLessThanOrEqualTo(900).isGreaterThan(890);
    }

    @Test
    void getExpirationSeconds_matchesConfiguredExpiration() {
        assertThat(jwtUtil.getExpirationSeconds()).isEqualTo(900);
    }
}