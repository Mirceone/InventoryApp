package com.mirceone.inventoryapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final DocumentUploadRateLimitFilter documentUploadRateLimitFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final OpsApiKeyFilter opsApiKeyFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            DocumentUploadRateLimitFilter documentUploadRateLimitFilter,
            AuthRateLimitFilter authRateLimitFilter,
            OpsApiKeyFilter opsApiKeyFilter,
            @Value("${app.security.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins
    ) {
        this.documentUploadRateLimitFilter = documentUploadRateLimitFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.opsApiKeyFilter = opsApiKeyFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/auth/**", "/ops/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(opsApiKeyFilter, AuthRateLimitFilter.class);
        http.addFilterBefore(documentUploadRateLimitFilter, AuthRateLimitFilter.class);

        // Validează Bearer token-ul JWT și îl pune în SecurityContext.
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
