package com.mirceone.inventoryapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.api.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class OpsApiKeyFilter extends OncePerRequestFilter {

    static final String OPS_API_KEY_HEADER = "X-Ops-Api-Key";

    private final String configuredApiKey;
    private final ObjectMapper objectMapper;

    public OpsApiKeyFilter(
            @Value("${app.ops.api-key:}") String configuredApiKey,
            ObjectMapper objectMapper
    ) {
        this.configuredApiKey = configuredApiKey == null ? "" : configuredApiKey.trim();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/ops");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (configuredApiKey.isBlank()) {
            writeError(
                    request,
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPS_API_DISABLED",
                    "Ops API key is not configured"
            );
            return;
        }

        String requestApiKey = request.getHeader(OPS_API_KEY_HEADER);
        if (!configuredApiKey.equals(requestApiKey)) {
            writeError(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_OPS_API_KEY",
                    "Invalid ops API key"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                null
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
