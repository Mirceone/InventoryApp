package com.mirceone.inventoryapp.service.email;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);

    void sendFirmInvitationEmail(String toEmail, String firmName, String inviteLink, String roleDisplayLabel);

    void sendCriticalFirmStatusEmail(String toEmail, String firmName, String statusDisplayLabel, String message);

    void sendOwnershipTransferConfirmationCodeEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName,
            String confirmationCode,
            long expiresInMinutes
    );

    void sendOwnershipTransferCompletedForPreviousOwnerEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName
    );

    void sendOwnershipTransferCompletedForNewOwnerEmail(
            String toEmail,
            String firmName,
            String previousOwnerDisplayName
    );
}
