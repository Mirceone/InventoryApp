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
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
                <p>Ai solicitat resetarea parolei pentru InventoryApp.</p>
                <p><a href="%s">Reseteaza parola</a></p>
                <p>Link-ul expira in curand. Daca nu ai facut tu aceasta cerere, ignora acest email.</p>
                """.formatted(resetLink);

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(toEmail)
                .subject("Resetare parola InventoryApp")
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(options);
            log.debug("Password reset email sent to {} (id={})", toEmail, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Unable to send password reset email");
        }
    }
}
