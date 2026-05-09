package com.whatsnext.authapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Stamps every request with a correlation id, exposed both to the client (via the
 * {@code X-Request-Id} response header) and to the server-side log pipeline (via SLF4J MDC,
 * key {@value #MDC_KEY}).
 *
 * <p>If the caller already supplied an {@code X-Request-Id} we propagate it as-is (after
 * a length cap) so distributed traces stay coherent. Otherwise we mint a fresh UUID.
 *
 * <p>Runs at the highest precedence so the id is in MDC before any other filter or handler
 * logs anything, and is removed in {@code finally} so the value never leaks across pooled
 * threads.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final int MAX_INCOMING_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String sanitize(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return null;
        }
        String trimmed = incoming.trim();
        return trimmed.length() > MAX_INCOMING_LENGTH ? trimmed.substring(0, MAX_INCOMING_LENGTH) : trimmed;
    }
}
