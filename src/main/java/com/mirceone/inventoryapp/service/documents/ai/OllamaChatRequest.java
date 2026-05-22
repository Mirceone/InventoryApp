package com.mirceone.inventoryapp.service.documents.ai;

import java.util.List;

record OllamaChatRequest(
        String model,
        boolean stream,
        List<OllamaMessage> messages,
        String format
) {
    record OllamaMessage(String role, String content) {
    }
}
