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
public class RateLimitFilter extends OncePerRequestFilter{
	
	@Autowired
    private ProxyManager<byte[]> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
    	
    	String uri = request.getRequestURI();
    	
    	if (request.getMethod().equals("POST")) {
    		
    		Supplier<BucketConfiguration> configSupplier = null;
            String prefix = "";
            
            if (uri.equals("/api/users/login")) {
                configSupplier = getConfigSupplierForLogin();
                prefix = "login_limit:";
            } else if (uri.equals("/api/users/register")) {
                configSupplier = getConfigSupplierForRegister();
                prefix = "register_limit:";
            }
            if (configSupplier != null) {
                String ip = getClientIP(request);
                byte[] key = (prefix + ip).getBytes();

                Bucket bucket = proxyManager.builder().build(key, configSupplier);

                if (!bucket.tryConsume(1)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    
                    String errorMsg = uri.contains("login") 
                        ? "Too many login attempts. Please try again in a minute." 
                        : "Too many accounts created from this IP. Please try again in an hour.";
                        
                    response.getWriter().write(String.format("{\"error\": \"%s\"}", errorMsg));
                    return; 
                }
            }
    	}
  
        filterChain.doFilter(request, response);
    }
    
    private Supplier<BucketConfiguration> getConfigSupplierForLogin() {
        return () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillGreedy(5, Duration.ofMinutes(1))
                    .build();
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }
    
    private Supplier<BucketConfiguration> getConfigSupplierForRegister() {
        return () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(3) // 3
                    .refillGreedy(3, Duration.ofHours(1)) 
                    .build();
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
