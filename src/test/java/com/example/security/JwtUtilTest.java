package com.example.security;

import com.example.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {
	/**

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

        String extracted = jwtUtil.extractUsername(token);

        assertThat(extracted).isEqualTo("john");
    }

    @Test
    void generateToken_and_extractRole_returnsSameRole() {
        String token = jwtUtil.generateToken("admin", Role.ADMIN);

        String role = jwtUtil.extractRole(token);

        assertThat(role).isEqualTo("ADMIN");
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
    **/
}