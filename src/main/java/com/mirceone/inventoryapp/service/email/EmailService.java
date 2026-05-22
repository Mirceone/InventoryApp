package com.mirceone.inventoryapp.service.email;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);
}
