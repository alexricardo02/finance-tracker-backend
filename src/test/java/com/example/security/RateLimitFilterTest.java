package com.example.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import java.io.PrintWriter;
import io.github.bucket4j.distributed.BucketProxy;
import java.io.StringWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private ProxyManager<byte[]> proxyManager;
    @Mock private RemoteBucketBuilder<byte[]> bucketBuilder;
    @Mock private BucketProxy bucket;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    @InjectMocks private RateLimitFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

	private void stubBucket(boolean consumed) {
		when(proxyManager.builder()).thenReturn(bucketBuilder);
		// WHY: RemoteBucketBuilder has multiple build() overloads. We use explicit
		// class matchers
		// so the Java compiler knows exactly which method signature we are mocking.
		when(bucketBuilder.build(any(byte[].class), any(Supplier.class))).thenReturn(bucket);

		// WHY: Bucket4j v8+ tracks both 'nanosToWaitForRefill' and
		// 'nanosToWaitForReset'.
		// The rejected() factory requires 3 parameters to simulate this accurately.
		ConsumptionProbe probe = consumed ? ConsumptionProbe.consumed(0, 0)
				: ConsumptionProbe.rejected(0, 5_000_000_000L, 5_000_000_000L);
		when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
	}

    @Test
    void doFilter_loginWithinLimit_continuesChain() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/users/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        stubBucket(true);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilter_loginExceedsLimit_returns429_doesNotContinueChain() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/users/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        stubBucket(false);

        filter.doFilterInternal(request, response, chain);
        
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

        verify(response).setHeader(eq("Retry-After"), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_nonRateLimitedGetRequest_skipsBucketEntirely() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/incomes");

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(proxyManager);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_authenticatedMutation_appliesPerUserBucket() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/incomes");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", null, List.of()));
        stubBucket(true);

        filter.doFilterInternal(request, response, chain);

        verify(proxyManager).builder();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_anonymousMutation_skipsPerUserBucket_stillContinues() throws Exception {
        // WHY: getAuthenticatedUsername() returns null pre-JwtRequestFilter auth setup;
        // filter must not NPE and must still let the request through to Spring Security.
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/incomes");

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(proxyManager);
        verify(chain).doFilter(request, response);
    }
}