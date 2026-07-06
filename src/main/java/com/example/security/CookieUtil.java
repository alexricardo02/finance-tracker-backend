package com.example.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public ResponseCookie buildAccessTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("auth_token", token)
                .httpOnly(true).secure(true).sameSite("None").path("/")
                .maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie buildRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true).secure(true).sameSite("None").path("/")
                .maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).sameSite("None").path("/")
                .maxAge(0).build();
    }
}