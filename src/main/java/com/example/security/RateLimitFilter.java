package com.example.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	@Autowired
	private ProxyManager<byte[]> proxyManager;
	
	private Supplier<BucketConfiguration> getConfigSupplierForForgotPassword() {
	    return () -> {
	        Bandwidth limit = Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofHours(1)).build();
	        return BucketConfiguration.builder().addLimit(limit).build();
	    };
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	        throws ServletException, IOException {

	    String uri = request.getRequestURI();
	    String method = request.getMethod();

	    Supplier<BucketConfiguration> configSupplier = null;
	    String prefix = "";
	    String rateLimitId = null;

	    if (method.equals("POST")) {
	        if (uri.equals("/api/users/login")) {
	            configSupplier = getConfigSupplierForLogin();
	            prefix = "login_limit:";
	            rateLimitId = getClientIP(request);
	        } else if (uri.equals("/api/users/register")) {
	            configSupplier = getConfigSupplierForRegister();
	            prefix = "register_limit:";
	            rateLimitId = getClientIP(request);
	        } else if (uri.equals("/api/users/refresh")) {
	            // NUEVO: sin esto, /refresh podía ser golpeado sin límite
	            configSupplier = getConfigSupplierForRefresh();
	            prefix = "refresh_limit:";
	            rateLimitId = getClientIP(request);
	        } else if (uri.equals("/api/users/forgot-password")) {
	            configSupplier = getConfigSupplierForForgotPassword();
	            prefix = "forgot_password_limit:";
	            rateLimitId = getClientIP(request);
	        }
	        
	    }

	    boolean isMutationEndpoint = (uri.startsWith("/api/incomes") || uri.startsWith("/api/expenses")
	            || uri.startsWith("/api/categories") || uri.startsWith("/api/settings")
	            || uri.startsWith("/api/imports"))
	            && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE"));

	    if (isMutationEndpoint) {
	        String username = getAuthenticatedUsername();
	        if (username != null) {
	            checkAndConsume(response, "mutation_limit:", username, getConfigSupplierForMutations());
	            if (response.isCommitted())
	                return;
	        }
	    }

	    if (configSupplier != null) {
	        checkAndConsume(response, prefix, rateLimitId, configSupplier);
	        if (response.isCommitted())
	            return;
	    }

	    filterChain.doFilter(request, response);
	}

	private void checkAndConsume(HttpServletResponse response, String prefix, String id,
	        Supplier<BucketConfiguration> configSupplier) throws IOException {
	    byte[] key = (prefix + id).getBytes();
	    Bucket bucket = proxyManager.builder().build(key, configSupplier);

	    var probe = bucket.tryConsumeAndReturnRemaining(1);
	    if (!probe.isConsumed()) {
	        long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
	        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
	        response.setHeader("Retry-After", String.valueOf(waitSeconds));
	        response.setContentType("application/json");
	        response.getWriter().write("{\"error\": \"Too many requests. Please slow down.\", \"retryAfterSeconds\": " + waitSeconds + "}");
	    }
	}

	private Supplier<BucketConfiguration> getConfigSupplierForRefresh() {
	    return () -> {
	        Bandwidth limit = Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build();
	        return BucketConfiguration.builder().addLimit(limit).build();
	    };
	}

	private String getAuthenticatedUsername() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
	}

	private Supplier<BucketConfiguration> getConfigSupplierForMutations() {
		return () -> {
			Bandwidth limit = Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build();
			return BucketConfiguration.builder().addLimit(limit).build();
		};
	}

	private Supplier<BucketConfiguration> getConfigSupplierForLogin() {
		return () -> {
			Bandwidth limit = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build();
			return BucketConfiguration.builder().addLimit(limit).build();
		};
	}

	private Supplier<BucketConfiguration> getConfigSupplierForRegister() {
		return () -> {
			Bandwidth limit = Bandwidth.builder().capacity(3) // 3
					.refillGreedy(3, Duration.ofHours(1)).build();
			return BucketConfiguration.builder().addLimit(limit).build();
		};
	}

	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0].trim(); // Extrae la IP real del usuario
	}

}
