package com.example.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuditingConfigTest {

    private final AuditingConfig config = new AuditingConfig();
    private final AuditorAware<String> auditorProvider = config.auditorProvider();

    @AfterEach
    void tearDown() {
        // WHY: The SecurityContext is thread-local. Failing to clear it leaks state into other tests,
        // causing unpredictable, cascading test failures in CI/CD pipelines.
        SecurityContextHolder.clearContext();
    }

    @Test
    void provideAuditor_returnsSystem_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("SYSTEM");
    }

    @Test
    void provideAuditor_returnsUsername_whenAuthenticated() {
    	UsernamePasswordAuthenticationToken auth = 
    			new UsernamePasswordAuthenticationToken("alex_admin", "password", java.util.List.of());
    	SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("alex_admin");
    }
}