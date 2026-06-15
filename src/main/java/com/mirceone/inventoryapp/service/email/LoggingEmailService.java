package com.mirceone.inventoryapp.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("[password-reset] to={} link={}", toEmail, resetLink);
    }

    @Override
    public void sendFirmInvitationEmail(String toEmail, String firmName, String inviteLink, String roleDisplayLabel) {
        log.info("[firm-invitation] to={} firm={} role={} link={}", toEmail, firmName, roleDisplayLabel, inviteLink);
    }

    @Override
    public void sendCriticalFirmStatusEmail(String toEmail, String firmName, String statusDisplayLabel, String message) {
        log.info("[firm-status-critical] to={} firm={} status={} message={}",
                toEmail, firmName, statusDisplayLabel, message);
    }

    @Override
    public void sendOwnershipTransferConfirmationCodeEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName,
            String confirmationCode,
            long expiresInMinutes
    ) {
        log.info("[ownership-transfer-confirmation] to={} firm={} promoted={} code={} expiresInMinutes={}",
                toEmail, firmName, promotedDisplayName, confirmationCode, expiresInMinutes);
    }

    @Override
    public void sendOwnershipTransferCompletedForPreviousOwnerEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName
    ) {
        log.info("[ownership-transfer-complete-previous-owner] to={} firm={} promoted={}",
                toEmail, firmName, promotedDisplayName);
    }

    @Override
    public void sendOwnershipTransferCompletedForNewOwnerEmail(
            String toEmail,
            String firmName,
            String previousOwnerDisplayName
    ) {
        log.info("[ownership-transfer-complete-new-owner] to={} firm={} previousOwner={}",
                toEmail, firmName, previousOwnerDisplayName);
    }
}
