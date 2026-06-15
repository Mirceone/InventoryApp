package com.mirceone.inventoryapp.service.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final Resend resend;
    private final String fromAddress;

    public ResendEmailService(String apiKey, String fromAddress) {
        this.resend = new Resend(apiKey);
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendFirmInvitationEmail(String toEmail, String firmName, String inviteLink, String roleDisplayLabel) {
        String html = """
                <p>Ai fost invitat sa te alaturi echipei <strong>%s</strong> pe InventoryApp.</p>
                <p>Rol: %s</p>
                <p><a href="%s">Accepta invitatia</a></p>
                <p>Link-ul expira in curand. Daca nu te asteptai aceasta invitatie, ignora acest email.</p>
                """.formatted(firmName, roleDisplayLabel, inviteLink);

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(toEmail)
                .subject("Invitatie echipa InventoryApp - " + firmName)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.debug("Firm invitation email sent to {} (id={})", toEmail, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send firm invitation email to {}", toEmail, e);
            throw invitationEmailFailed(e);
        } catch (RuntimeException e) {
            log.error("Failed to send firm invitation email to {}", toEmail, e);
            throw invitationEmailFailed(e);
        }
    }

    @Override
    public void sendCriticalFirmStatusEmail(String toEmail, String firmName, String statusDisplayLabel, String message) {
        String extra = (message == null || message.isBlank())
                ? ""
                : "<p>Detalii: %s</p>".formatted(message.strip());
        String html = """
                <p>Firma <strong>%s</strong> are acum statusul <strong>%s</strong> in InventoryApp.</p>
                %s
                <p>Verifica imediat starea firmei si istoricul de status din aplicatie.</p>
                """.formatted(firmName, statusDisplayLabel, extra);

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(toEmail)
                .subject("Alerta status firma InventoryApp - " + firmName)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.debug("Critical firm status email sent to {} (id={})", toEmail, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send critical firm status email to {}", toEmail, e);
            throw emailFailed("Unable to send critical firm status email", e);
        } catch (RuntimeException e) {
            log.error("Failed to send critical firm status email to {}", toEmail, e);
            throw emailFailed("Unable to send critical firm status email", e);
        }
    }

    @Override
    public void sendOwnershipTransferConfirmationCodeEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName,
            String confirmationCode,
            long expiresInMinutes
    ) {
        String html = """
                <p>Ai cerut transferul ownership-ului pentru firma <strong>%s</strong>.</p>
                <p>Noul owner propus: <strong>%s</strong></p>
                <p>Codul tău de confirmare este:</p>
                <p style="font-size:28px;font-weight:700;letter-spacing:6px;">%s</p>
                <p>Codul expiră în %d minute. Dacă nu ai inițiat tu acest transfer, ignoră email-ul.</p>
                """.formatted(firmName, promotedDisplayName, confirmationCode, expiresInMinutes);

        sendEmail(
                toEmail,
                "Confirmare transfer ownership - " + firmName,
                html,
                "Unable to send ownership transfer confirmation email"
        );
    }

    @Override
    public void sendOwnershipTransferCompletedForPreviousOwnerEmail(
            String toEmail,
            String firmName,
            String promotedDisplayName
    ) {
        String html = """
                <p>Transferul ownership-ului pentru firma <strong>%s</strong> a fost finalizat.</p>
                <p>Noul owner este acum <strong>%s</strong>.</p>
                <p>Contul tău a rămas activ în firmă cu rolul de membru.</p>
                """.formatted(firmName, promotedDisplayName);

        sendEmail(
                toEmail,
                "Ownership transfer finalizat - " + firmName,
                html,
                "Unable to send ownership transfer completion email to previous owner"
        );
    }

    @Override
    public void sendOwnershipTransferCompletedForNewOwnerEmail(
            String toEmail,
            String firmName,
            String previousOwnerDisplayName
    ) {
        String html = """
                <p>Ai fost promovat ca owner pentru firma <strong>%s</strong>.</p>
                <p>Ownership-ul a fost transferat de <strong>%s</strong>.</p>
                <p>Acum poți administra statusul firmei, membrii și setările sensibile.</p>
                """.formatted(firmName, previousOwnerDisplayName);

        sendEmail(
                toEmail,
                "Ai devenit owner - " + firmName,
                html,
                "Unable to send ownership transfer completion email to new owner"
        );
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
                <p>Ai solicitat resetarea parolei pentru InventoryApp.</p>
                <p><a href="%s">Reseteaza parola</a></p>
                <p>Link-ul expira in curand. Daca nu ai facut tu aceasta cerere, ignora acest email.</p>
                """.formatted(resetLink);

        sendEmail(toEmail, "Resetare parola InventoryApp", html, "Unable to send password reset email");
    }

    private static ResponseStatusException invitationEmailFailed(Exception cause) {
        return emailFailed(
                "Unable to send invitation email. On Resend test mode you can only send to your account email until a domain is verified.",
                cause
        );
    }

    private void sendEmail(String toEmail, String subject, String html, String fallbackMessage) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(toEmail)
                .subject(subject)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.debug("Email sent to {} subject={} (id={})", toEmail, subject, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send email to {} subject={}", toEmail, subject, e);
            throw emailFailed(fallbackMessage, e);
        } catch (RuntimeException e) {
            log.error("Failed to send email to {} subject={}", toEmail, subject, e);
            throw emailFailed(fallbackMessage, e);
        }
    }

    private static ResponseStatusException emailFailed(String fallback, Exception cause) {
        String detail = cause.getMessage();
        if (detail != null && !detail.isBlank()) {
            return new ResponseStatusException(SERVICE_UNAVAILABLE, detail.strip());
        }
        return new ResponseStatusException(SERVICE_UNAVAILABLE, fallback);
    }
}
