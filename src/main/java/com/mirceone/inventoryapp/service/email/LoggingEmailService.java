package com.mirceone.inventoryapp.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("[password-reset] to={} link={}", toEmail, resetLink);
    }
}
