package com.example.ragchat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting. Key by X-User-Id if present, otherwise client IP.
 * Fixed window: max N requests per window (e.g. 100 per 60 seconds).
 */
@Component
@Order(2) // After RequestIdFilter (1)
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";
    private static final String RATE_LIMIT_RESPONSE_BODY = "{\"timestamp\":\"\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\",\"path\":\"\"}";

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;

    // Lua: INCR key, set TTL only on first request in window, return current count
    private static final String LUA_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.max-requests:100}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Skip rate limit for health checks
        if (request.getRequestURI() != null && request.getRequestURI().startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        String key = RATE_LIMIT_KEY_PREFIX + clientKey;

        Long count = redisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
                List.of(key),
                String.valueOf(windowSeconds)
        );

        if (count != null && count > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(RATE_LIMIT_RESPONSE_BODY.replace("\"path\":\"\"", "\"path\":\"" + request.getRequestURI() + "\""));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return "ip:" + (remote != null ? remote : "unknown");
    }
}
