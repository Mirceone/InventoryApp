package com.mirceone.inventoryapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirceone.inventoryapp.api.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Rate limits POST uploads to work-order file and invoice upload endpoints.
 */
@Component
public class DocumentUploadRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern FIRM_UPLOAD_POST =
            Pattern.compile("^/firms/[0-9a-fA-F\\-]{36}/work-orders/[0-9a-fA-F\\-]{36}/(files|invoices)(/batch)?$");

    private final AuthRateLimiter documentUploadRateLimiter;
    private final ObjectMapper objectMapper;

    public DocumentUploadRateLimitFilter(
            @Qualifier("documentUploadRateLimiter") AuthRateLimiter documentUploadRateLimiter,
            ObjectMapper objectMapper
    ) {
        this.documentUploadRateLimiter = documentUploadRateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !FIRM_UPLOAD_POST.matcher(uri).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        if (!documentUploadRateLimiter.allow(key)) {
            writeTooManyRequestsResponse(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "RATE_LIMIT_EXCEEDED",
                "Too many document uploads. Please try again later.",
                request.getRequestURI(),
                null
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
