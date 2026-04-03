package com.mirceone.inventoryapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.api.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REFRESH_PATH = "/auth/refresh";

    private final AuthRateLimiter authRateLimiter;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(AuthRateLimiter authRateLimiter, ObjectMapper objectMapper) {
        this.authRateLimiter = authRateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isAuthRateLimitedEndpoint(request)) {
            String key = request.getRemoteAddr() + "|" + request.getRequestURI();
            if (!authRateLimiter.allow(key)) {
                writeTooManyRequestsResponse(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthRateLimitedEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return LOGIN_PATH.equals(uri) || REFRESH_PATH.equals(uri);
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "RATE_LIMIT_EXCEEDED",
                "Too many authentication requests. Please try again later.",
                request.getRequestURI(),
                null
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
