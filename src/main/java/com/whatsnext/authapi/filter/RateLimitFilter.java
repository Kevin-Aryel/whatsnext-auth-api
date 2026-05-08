package com.whatsnext.authapi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsnext.authapi.config.RateLimitConfig;
import com.whatsnext.authapi.dto.response.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = extractClientIp(request);

        RateLimitConfig.EndpointConfig matchedConfig = switch (path) {
            case "/api/v1/auth/login"    -> rateLimitConfig.getLogin();
            case "/api/v1/auth/register" -> rateLimitConfig.getRegister();
            case "/api/v1/auth/refresh"  -> rateLimitConfig.getRefresh();
            default                      -> null;
        };

        Map<String, Bucket> targetBuckets = switch (path) {
            case "/api/v1/auth/login"    -> loginBuckets;
            case "/api/v1/auth/register" -> registerBuckets;
            case "/api/v1/auth/refresh"  -> refreshBuckets;
            default                      -> null;
        };

        Bucket bucket = null;
        if (matchedConfig != null && targetBuckets != null) {
            bucket = targetBuckets.computeIfAbsent(ip, k -> buildBucket(matchedConfig));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After",
                    String.valueOf(matchedConfig.getRefillSeconds()));
            ErrorResponse error = ErrorResponse.of(
                    "429", "Too Many Requests", "Rate limit exceeded. Try again later.");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket buildBucket(RateLimitConfig.EndpointConfig config) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(config.getCapacity(),
                        Duration.ofSeconds(config.getRefillSeconds())))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For is client-controlled and trivially spoofable when read here:
        // a fresh value per request gives each fake IP its own bucket, bypassing the limit.
        // Render's edge keeps the immediate remote address tamper-proof; if the app moves
        // behind another proxy, configure server.forward-headers-strategy=native so Tomcat's
        // RemoteIpValve rewrites getRemoteAddr() from a vetted X-Forwarded-For only when the
        // hop comes from a trusted proxy CIDR.
        return request.getRemoteAddr();
    }
}
