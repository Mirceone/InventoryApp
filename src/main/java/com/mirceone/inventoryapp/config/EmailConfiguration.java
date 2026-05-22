package com.mirceone.inventoryapp.config;

import com.mirceone.inventoryapp.service.email.EmailService;
import com.mirceone.inventoryapp.service.email.LoggingEmailService;
import com.mirceone.inventoryapp.service.email.ResendEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfiguration {

    @Bean
    public EmailService emailService(
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.resend.from}") String fromAddress
    ) {
        if (isResendConfigured(apiKey)) {
            return new ResendEmailService(apiKey.trim(), fromAddress);
        }
        return new LoggingEmailService();
    }

    private static boolean isResendConfigured(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        String trimmed = apiKey.trim();
        return trimmed.startsWith("re_") && !trimmed.equals("re_mock_replace_with_your_resend_api_key");
    }
}
