package com.example.ragchat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates API key from X-API-Key or Authorization: ApiKey <key> and sets authentication.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String AUTH_HEADER_PREFIX = "ApiKey ";

    private final String validApiKey;

    public ApiKeyAuthFilter(@Value("${app.api-key:}") String validApiKey) {
        this.validApiKey = validApiKey != null ? validApiKey : "";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith(AUTH_HEADER_PREFIX)) {
                apiKey = auth.substring(AUTH_HEADER_PREFIX.length()).trim();
            }
        }

        if (apiKey != null && !apiKey.isBlank() && apiKey.equals(validApiKey)) {
            SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthenticationToken(apiKey));
        } else {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
